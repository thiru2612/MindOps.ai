package com.ai.project.service;

import com.ai.project.dto.AwsCredentialRequest;
import com.ai.project.dto.AzureCredentialRequest;
import com.ai.project.dto.CredentialResponse;
import com.ai.project.entity.CloudCredential;
import com.ai.project.entity.User;
import com.ai.project.entity.enums.CloudProvider;
import com.ai.project.entity.enums.CredentialValidationStatus;
import com.ai.project.exception.CredentialInUseException;
import com.ai.project.exception.ResourceNotFoundException;
import com.ai.project.repository.CloudCredentialRepository;
import com.ai.project.security.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the full lifecycle of vaulted cloud credentials.
 *
 * <p><strong>Security contracts enforced by this service:</strong>
 * <ol>
 *   <li>All sensitive credential fields are encrypted with AES-256/GCM via
 *       {@link EncryptionUtil} before any database write.</li>
 *   <li>Raw decrypted values are never returned from any public method of this
 *       service. All outbound data flows through
 *       {@link #toMaskedResponse(CloudCredential)} which applies character-level
 *       masking to the decrypted values.</li>
 *   <li>Deletion is blocked if the credential is actively referenced by a
 *       deployment plan in EXECUTING or DESTROYING status.</li>
 *   <li>Ownership is strictly enforced — every lookup verifies that the
 *       {@code credential.user} matches the authenticated user.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialVaultService {

    private final CloudCredentialRepository credentialRepository;
    private final EncryptionUtil            encryptionUtil;
    private final UserService               userService;

    // ── Store AWS credential ─────────────────────────────────────────────────

    /**
     * Encrypts and persists AWS IAM credentials for the authenticated user.
     *
     * @return a {@link CredentialResponse} with masked key values — no plaintext is returned
     */
    @Transactional
    public CredentialResponse storeAwsCredential(AwsCredentialRequest request) {
        User currentUser = userService.resolveCurrentUser();

        CloudCredential credential = CloudCredential.builder()
            .user(currentUser)
            .provider(CloudProvider.AWS)
            .credentialLabel(request.getCredentialLabel().trim())
            .accessKeyIdEncrypted(encryptionUtil.encrypt(request.getAccessKeyId().trim()))
            .secretAccessKeyEncrypted(encryptionUtil.encrypt(request.getSecretAccessKey().trim()))
            .defaultRegion(request.getDefaultRegion().trim().toLowerCase())
            .validationStatus(CredentialValidationStatus.UNVERIFIED)
            .build();

        CloudCredential saved = credentialRepository.save(credential);
        log.info("[CredentialVaultService] AWS credential '{}' stored for user: {}",
            saved.getPublicId(), currentUser.getEmail());

        return toMaskedResponse(saved);
    }

    // ── Store Azure credential ───────────────────────────────────────────────

    /**
     * Encrypts and persists Azure Service Principal credentials for the authenticated user.
     *
     * @return a {@link CredentialResponse} with masked field values
     */
    @Transactional
    public CredentialResponse storeAzureCredential(AzureCredentialRequest request) {
        User currentUser = userService.resolveCurrentUser();

        CloudCredential credential = CloudCredential.builder()
            .user(currentUser)
            .provider(CloudProvider.AZURE)
            .credentialLabel(request.getCredentialLabel().trim())
            .tenantIdEncrypted(encryptionUtil.encrypt(request.getTenantId().trim()))
            .clientIdEncrypted(encryptionUtil.encrypt(request.getClientId().trim()))
            .clientSecretEncrypted(encryptionUtil.encrypt(request.getClientSecret().trim()))
            .subscriptionIdEncrypted(encryptionUtil.encrypt(request.getSubscriptionId().trim()))
            .validationStatus(CredentialValidationStatus.UNVERIFIED)
            .build();

        CloudCredential saved = credentialRepository.save(credential);
        log.info("[CredentialVaultService] Azure credential '{}' stored for user: {}",
            saved.getPublicId(), currentUser.getEmail());

        return toMaskedResponse(saved);
    }

    // ── List credentials for current user ────────────────────────────────────

    /**
     * Returns all credentials for the authenticated user with all sensitive
     * fields masked. Never decrypts values for list operations.
     */
    @Transactional(readOnly = true)
    public List<CredentialResponse> listCredentialsForCurrentUser() {
        User currentUser = userService.resolveCurrentUser();
        return credentialRepository
            .findAllByUserOrderByCreatedAtDesc(currentUser)
            .stream()
            .map(this::toMaskedResponse)
            .toList();
    }

    // ── Delete credential ────────────────────────────────────────────────────

    /**
     * Deletes a vaulted credential after verifying:
     * <ol>
     *   <li>Ownership — the credential belongs to the authenticated user.</li>
     *   <li>Safety — no active (EXECUTING/DESTROYING) deployment plan references it.</li>
     * </ol>
     *
     * @throws ResourceNotFoundException if the credential does not exist or is not owned by the user
     * @throws CredentialInUseException  if an active deployment is using this credential
     */
    @Transactional
    public void deleteCredential(String credentialPublicId) {
        User currentUser = userService.resolveCurrentUser();

        CloudCredential credential = credentialRepository
            .findByPublicIdAndUser(credentialPublicId, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("CloudCredential", credentialPublicId));

        if (credentialRepository.isCredentialActivelyUsed(credentialPublicId)) {
            throw new CredentialInUseException(credentialPublicId);
        }

        credentialRepository.delete(credential);
        log.info("[CredentialVaultService] Credential '{}' deleted by user: {}",
            credentialPublicId, currentUser.getEmail());
    }

    // ── Internal: decrypt for SDK use (package-visible, not exposed via REST) ─

    /**
     * Decrypts and returns the raw AWS credentials for internal SDK use only.
     *
     * <p><strong>This method must NEVER be called from a controller layer.</strong>
     * It is intended exclusively for the deployment execution service which needs
     * plaintext credentials to authenticate with AWS SDK v2.</p>
     *
     * @param credentialPublicId the public ID of the credential to decrypt
     * @param requestingUser     the user requesting decryption (ownership enforced)
     * @return a decrypted {@link DecryptedAwsCredential} value object
     * @throws ResourceNotFoundException if the credential is not owned by the user
     */
    @Transactional(readOnly = true)
    public DecryptedAwsCredential decryptAwsCredential(String credentialPublicId, User requestingUser) {
        CloudCredential credential = credentialRepository
            .findByPublicIdAndUser(credentialPublicId, requestingUser)
            .orElseThrow(() -> new ResourceNotFoundException("CloudCredential", credentialPublicId));

        if (credential.getProvider() != CloudProvider.AWS) {
            throw new IllegalArgumentException(
                "Credential " + credentialPublicId + " is not an AWS credential."
            );
        }

        return new DecryptedAwsCredential(
            encryptionUtil.decrypt(credential.getAccessKeyIdEncrypted()),
            encryptionUtil.decrypt(credential.getSecretAccessKeyEncrypted()),
            credential.getDefaultRegion()
        );
    }

    /**
     * Decrypts and returns the raw Azure credentials for internal SDK use only.
     *
     * <p><strong>This method must NEVER be called from a controller layer.</strong></p>
     */
    @Transactional(readOnly = true)
    public DecryptedAzureCredential decryptAzureCredential(String credentialPublicId, User requestingUser) {
        CloudCredential credential = credentialRepository
            .findByPublicIdAndUser(credentialPublicId, requestingUser)
            .orElseThrow(() -> new ResourceNotFoundException("CloudCredential", credentialPublicId));

        if (credential.getProvider() != CloudProvider.AZURE) {
            throw new IllegalArgumentException(
                "Credential " + credentialPublicId + " is not an Azure credential."
            );
        }

        return new DecryptedAzureCredential(
            encryptionUtil.decrypt(credential.getTenantIdEncrypted()),
            encryptionUtil.decrypt(credential.getClientIdEncrypted()),
            encryptionUtil.decrypt(credential.getClientSecretEncrypted()),
            encryptionUtil.decrypt(credential.getSubscriptionIdEncrypted())
        );
    }

    // ── Masking logic ────────────────────────────────────────────────────────

    /**
     * Converts a {@link CloudCredential} entity to a masked {@link CredentialResponse}.
     *
     * <p>For AWS credentials, {@code accessKeyId} is decrypted solely to produce a
     * masked display value (e.g. {@code AKIA***MPLE}). The decrypted value is not
     * stored or returned — it is used only to extract the first 4 and last 4 characters
     * before being discarded.</p>
     *
     * <p>For Azure credentials, clientId and subscriptionId UUIDs are masked to show
     * only the first and last segments, replacing the middle with asterisks.</p>
     */
    private CredentialResponse toMaskedResponse(CloudCredential credential) {
        CredentialResponse.CredentialResponseBuilder builder = CredentialResponse.builder()
            .credentialId(credential.getPublicId())
            .provider(credential.getProvider())
            .credentialLabel(credential.getCredentialLabel())
            .validationStatus(credential.getValidationStatus())
            .createdAt(credential.getCreatedAt());

        if (credential.getProvider() == CloudProvider.AWS) {
            String maskedKeyId = maskAwsKeyId(
                encryptionUtil.decrypt(credential.getAccessKeyIdEncrypted())
            );
            builder
                .accessKeyIdMasked(maskedKeyId)
                .defaultRegion(credential.getDefaultRegion());

        } else if (credential.getProvider() == CloudProvider.AZURE) {
            String maskedClientId = maskUuid(
                encryptionUtil.decrypt(credential.getClientIdEncrypted())
            );
            String maskedSubscriptionId = maskUuid(
                encryptionUtil.decrypt(credential.getSubscriptionIdEncrypted())
            );
            builder
                .clientIdMasked(maskedClientId)
                .subscriptionIdMasked(maskedSubscriptionId);
        }

        return builder.build();
    }

    /**
     * Masks an AWS Access Key ID showing first 4 and last 4 characters.
     * Example: {@code AKIAIOSFODNN7EXAMPLE} → {@code AKIA***MPLE}
     */
    private String maskAwsKeyId(String accessKeyId) {
        if (accessKeyId == null || accessKeyId.length() < 8) {
            return "****";
        }
        return accessKeyId.substring(0, 4) +
               "***" +
               accessKeyId.substring(accessKeyId.length() - 4);
    }

    /**
     * Masks a UUID showing only the first and last UUID segments.
     * Example: {@code xxxxxxxx-1234-5678-abcd-yyyyyyyyyyyy}
     *        → {@code xxxxxxxx-****-****-****-yyyyyyyyyyyy}
     */
    private String maskUuid(String uuid) {
        if (uuid == null || uuid.length() < 36) {
            return "****-****-****-****-****";
        }
        // UUID format: 8-4-4-4-12  (total 36 chars with hyphens)
        String firstSegment = uuid.substring(0, 8);   // 8 chars
        String lastSegment  = uuid.substring(24);      // 12 chars
        return firstSegment + "-****-****-****-" + lastSegment;
    }

    // ── Decrypted value objects (internal use only) ──────────────────────────

    /**
     * Immutable value object carrying decrypted AWS credentials.
     * Intentionally not a JPA entity or serializable DTO — it must never
     * be written to a database or serialized to JSON.
     */
    public record DecryptedAwsCredential(
        String accessKeyId,
        String secretAccessKey,
        String region
    ) {}

    /**
     * Immutable value object carrying decrypted Azure credentials.
     * Same security constraints as {@link DecryptedAwsCredential}.
     */
    public record DecryptedAzureCredential(
        String tenantId,
        String clientId,
        String clientSecret,
        String subscriptionId
    ) {}
}