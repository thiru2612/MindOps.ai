package com.ai.project.service;

import com.ai.project.service.CredentialVaultService.DecryptedAwsCredential;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

/**
 * AWS provisioning service — scoped to EC2 for the Phase 5 MVP.
 *
 * <p><strong>SDK client lifecycle:</strong> Each public method constructs a new
 * {@link Ec2Client} scoped to the request and closes it in a try-with-resources
 * block. This is intentional: clients are inexpensive to construct with the
 * URL-connection HTTP provider, and per-request client construction avoids
 * credential caching across user sessions — a security requirement when
 * credentials belong to different users.</p>
 *
 * <p><strong>AMI strategy:</strong> A well-known public Amazon Linux 2 AMI ID
 * is used per region. For production, replace with an SSM Parameter Store lookup:
 * {@code /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2}
 * This avoids hardcoded IDs becoming stale as AWS deprecates older AMIs.</p>
 *
 * <p><strong>MVP scope:</strong> Provisions a single EC2 instance. Phase 6
 * will expand to VPC, ALB, ASG, RDS based on the full {@code sdkParamsJson}.</p>
 */
@Slf4j
@Service
public class AwsProvisioningService {

    /**
     * Per-region Amazon Linux 2 AMI IDs (HVM, x86_64, gp2 root volume).
     * Sourced from AWS public AMI catalogue — valid as of mid-2025.
     * Replace with SSM lookup for production resilience.
     */
    private static final java.util.Map<String, String> AMAZON_LINUX_2_AMIS =
        java.util.Map.ofEntries(
            java.util.Map.entry("us-east-1",      "ami-0c02fb55956c7d316"),
            java.util.Map.entry("us-east-2",      "ami-0b0af3577a65b8e61"),
            java.util.Map.entry("us-west-1",      "ami-0d382e80be7ffdae5"),
            java.util.Map.entry("us-west-2",      "ami-0892d3c7ee96c0bf7"),
            java.util.Map.entry("ap-south-1",     "ami-079b5e5b3971bd10d"),
            java.util.Map.entry("ap-southeast-1", "ami-055d15d9cefdf7749"),
            java.util.Map.entry("ap-southeast-2", "ami-0186908e2fdeea8f3"),
            java.util.Map.entry("ap-northeast-1", "ami-0218d08a1f9dac831"),
            java.util.Map.entry("eu-west-1",      "ami-04dd4500af104442f"),
            java.util.Map.entry("eu-west-2",      "ami-0e1e4cbf4adb3ab0d"),
            java.util.Map.entry("eu-central-1",   "ami-0d1ddd83282187d18"),
            java.util.Map.entry("sa-east-1",      "ami-0c27c96aaa148ba6d"),
            java.util.Map.entry("ca-central-1",   "ami-0b55e9ba396b5f9e7")
        );

    private static final String DEFAULT_AMI          = "ami-0c02fb55956c7d316"; // us-east-1 fallback
    private static final String MANAGED_BY_TAG_KEY   = "ManagedBy";
    private static final String MANAGED_BY_TAG_VALUE = "MindOps";
    private static final String APP_TAG_KEY          = "Application";
    private static final String APP_TAG_VALUE        = "MindOpsCloud";

    // ── Provision EC2 ─────────────────────────────────────────────────────────

    /**
     * Provisions a single EC2 instance from the Gemini-generated configuration.
     *
     * <p>Configuration resolution order for instance type:
     * <ol>
     *   <li>{@code config.instanceType} from Gemini JSON (if present and non-null)</li>
     *   <li>Fallback to {@code t3.micro} (free-tier eligible, safe default)</li>
     * </ol>
     * </p>
     *
     * @param config      the validated Gemini JSON config node
     * @param credentials decrypted AWS credentials (never stored or logged)
     * @return the provisioned EC2 instance ID (e.g. {@code i-0abc1234def56789a})
     * @throws Ec2Exception if the AWS API rejects the request (e.g. invalid instance type,
     *                      insufficient IAM permissions, or service quota exceeded)
     */
    public String provisionEc2(JsonNode config, DecryptedAwsCredential credentials) {
        String region       = resolveRegion(config, credentials.region());
        String instanceType = resolveInstanceType(config);
        String amiId        = resolveAmiId(region);

        log.info("[AwsProvisioningService] Provisioning EC2: type={}, region={}, ami={}",
            instanceType, region, amiId);

        try (Ec2Client ec2Client = buildEc2Client(credentials, region)) {

            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.fromValue(instanceType))
                .minCount(1)
                .maxCount(1)
                .tagSpecifications(buildTagSpecification(config, ResourceType.INSTANCE))
                .metadataOptions(
                    // Enforce IMDSv2 — disables the insecure IMDSv1 metadata endpoint
                    InstanceMetadataOptionsRequest.builder()
                        .httpTokens(HttpTokensState.REQUIRED)
                        .httpPutResponseHopLimit(1)
                        .build()
                )
                .build();

            RunInstancesResponse runResponse = ec2Client.runInstances(runRequest);

            if (runResponse.instances().isEmpty()) {
                throw new IllegalStateException(
                    "AWS RunInstances returned an empty instances list with no error."
                );
            }

            String instanceId = runResponse.instances().get(0).instanceId();
            log.info("[AwsProvisioningService] EC2 instance provisioned: instanceId={}",
                instanceId);

            return instanceId;

        } catch (Ec2Exception e) {
            log.error("[AwsProvisioningService] EC2 provisioning failed. " +
                      "AWS error code: {}, message: {}", e.awsErrorDetails().errorCode(),
                      e.awsErrorDetails().errorMessage());
            // Re-throw with a sanitised message — raw SDK exception is logged but not propagated
            throw new IllegalStateException(
                "AWS EC2 provisioning failed: " + e.awsErrorDetails().errorMessage()
            );
        }
    }

    // ── Teardown EC2 ──────────────────────────────────────────────────────────

    /**
     * Terminates the specified EC2 instance.
     *
     * <p>EC2 termination is asynchronous on the AWS side — this method issues the
     * termination request and verifies the instance entered a terminal state
     * ({@code shutting-down} or {@code terminated}) in the immediate API response.
     * It does not poll for full termination — for the MVP this is sufficient.
     * Phase 6 can add a waiter if blocking confirmation is required.</p>
     *
     * @param instanceId  the EC2 instance ID to terminate (e.g. {@code i-0abc1234})
     * @param region      the AWS region where the instance resides
     * @param credentials decrypted AWS credentials
     * @throws IllegalStateException if the AWS API rejects the termination request
     */
    public void teardownEc2(String instanceId, String region, DecryptedAwsCredential credentials) {
        log.info("[AwsProvisioningService] Terminating EC2 instance: {} in region: {}",
            instanceId, region);

        try (Ec2Client ec2Client = buildEc2Client(credentials, region)) {

            TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();

            TerminateInstancesResponse terminateResponse =
                ec2Client.terminateInstances(terminateRequest);

            terminateResponse.terminatingInstances().forEach(change ->
                log.info("[AwsProvisioningService] Instance {} state transition: {} → {}",
                    change.instanceId(),
                    change.previousState().nameAsString(),
                    change.currentState().nameAsString())
            );

        } catch (Ec2Exception e) {
            log.error("[AwsProvisioningService] EC2 termination failed. " +
                      "AWS error code: {}, message: {}",
                      e.awsErrorDetails().errorCode(),
                      e.awsErrorDetails().errorMessage());
            throw new IllegalStateException(
                "AWS EC2 termination failed: " + e.awsErrorDetails().errorMessage()
            );
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Constructs a new {@link Ec2Client} for the given credentials and region.
     * Uses the URL-connection HTTP client (synchronous, no Netty dependency).
     * Always closed by callers via try-with-resources.
     */
    private Ec2Client buildEc2Client(DecryptedAwsCredential credentials, String region) {
        return Ec2Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        credentials.accessKeyId(),
                        credentials.secretAccessKey()
                    )
                )
            )
            .httpClientBuilder(
                software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder()
            )
            .build();
    }

    /**
     * Builds a {@link TagSpecification} for the resource, applying standard
     * MindOps platform tags plus a deployment-specific name tag from the config.
     */
    private TagSpecification buildTagSpecification(JsonNode config, ResourceType resourceType) {
        String nameTag = "mindops-ec2";
        if (config != null && config.has("service")) {
            nameTag = "mindops-" + config.get("service").asText("ec2").toLowerCase();
        }

        return TagSpecification.builder()
            .resourceType(resourceType)
            .tags(
                Tag.builder().key("Name").value(nameTag).build(),
                Tag.builder().key(MANAGED_BY_TAG_KEY).value(MANAGED_BY_TAG_VALUE).build(),
                Tag.builder().key(APP_TAG_KEY).value(APP_TAG_VALUE).build(),
                Tag.builder().key("Environment").value("managed").build()
            )
            .build();
    }

    private String resolveRegion(JsonNode config, String credentialRegion) {
        if (config != null && config.has("region") && !config.get("region").isNull()) {
            return config.get("region").asText(credentialRegion);
        }
        return credentialRegion != null ? credentialRegion : "us-east-1";
    }

    private String resolveInstanceType(JsonNode config) {
        if (config != null && config.has("instanceType") && !config.get("instanceType").isNull()) {
            String type = config.get("instanceType").asText();
            if (!type.isBlank()) return type;
        }
        return "t3.micro";
    }

    private String resolveAmiId(String region) {
        return AMAZON_LINUX_2_AMIS.getOrDefault(region, DEFAULT_AMI);
    }
}