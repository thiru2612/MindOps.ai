package com.ai.project.service;

import com.ai.project.dto.GeminiResponse;
import com.ai.project.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * HTTP client service for the Google Gemini generative AI API.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Constructs the full Gemini request payload, prepending a strict
 *       system-level prompt guardrail to every user prompt.</li>
 *   <li>Calls the Gemini REST endpoint and maps the response envelope
 *       via {@link GeminiResponse}.</li>
 *   <li>Strips any markdown code fences (```json ... ```) from the response
 *       before JSON parsing, since some Gemini model versions wrap output
 *       in markdown blocks despite explicit instructions not to.</li>
 *   <li>Validates the parsed JSON against the guardrail contract — if Gemini
 *       returns a {@code FORBIDDEN_OPERATION} error field, throws
 *       {@link IllegalArgumentException} with code {@code GEMINI_GUARDRAIL_VIOLATION}.</li>
 *   <li>Exposes token usage metadata for the {@code AiComplianceLog}.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class GeminiClientService {

    private static final String SYSTEM_GUARDRAIL = """
        You are a strict Cloud Infrastructure Architect AI for the MindOps platform.
        
        ABSOLUTE CONSTRAINTS — violation of any of these rules will cause your response to be rejected:
        1. You are STRICTLY PROHIBITED from generating any configuration, command, parameter, \
        or instruction that DELETES, DROPS, TERMINATES, DESTROYS, DEPROVISIONS, or REMOVES \
        any cloud resource in any cloud provider (AWS, Azure, GCP, or any other).
        2. You may ONLY generate CREATE or UPDATE infrastructure configurations.
        3. If the user's request implies deletion, teardown, or destruction of any resource, \
        you MUST respond ONLY with this exact JSON and nothing else:
           {"error": "FORBIDDEN_OPERATION", "message": "Deletion operations are not permitted via this endpoint."}
        4. Your response MUST be a single, valid JSON object. No markdown code fences \
        (no ```json or ``` wrappers). No explanatory text before or after the JSON. \
        No conversational filler. Raw JSON only.
        5. The JSON object MUST contain exactly these fields:
           - "provider": string, one of ["AWS", "AZURE"]
           - "service": string (the primary cloud service, e.g. "EC2", "AzureContainerApps")
           - "region": string (the deployment region, e.g. "us-east-1", "eastus")
           - "instanceType": string or null (the compute tier if applicable)
           - "scalingConfig": object with "minInstances" (int) and "maxInstances" (int), or null
           - "storageConfig": object or null (e.g. {"type":"S3","sizGb":100} or null)
           - "networkConfig": object with "vpcRequired" (boolean) and "publicAccess" (boolean)
           - "estimatedMonthlyCostUsd": number (your best numeric estimate, no currency symbols)
           - "costBreakdown": array of objects, each with "resource" (string) and "costUsd" (number)
           - "reasoning": string (1-3 sentences explaining your configuration choices)
           - "tags": object of string key-value pairs for resource tagging (e.g. {"Environment":"production"})
        6. Do not include any field not listed above.
        
        USER REQUEST:
        """;

    private final RestClient    restClient;
    private final ObjectMapper  objectMapper;
    private final String        apiKey;
    private final String        endpoint;

    public GeminiClientService(
        @Value("${app.gemini.api-key}")       String apiKey,
        @Value("${app.gemini.endpoint}")      String endpoint,
        @Value("${app.gemini.timeout-seconds:30}") int timeoutSeconds,
        ObjectMapper objectMapper
    ) {
        this.apiKey       = apiKey;
        this.endpoint     = endpoint;
        this.objectMapper = objectMapper;

        this.restClient = RestClient.builder()
            .baseUrl(endpoint)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Sends a user prompt to the Gemini API with the system guardrail prepended,
     * and returns the parsed infrastructure configuration as a validated JSON string.
     *
     * @param userPrompt the raw natural language prompt from the user
     * @return a {@link GeminiResult} containing the validated JSON config string
     *         and token usage metadata
     * @throws IllegalArgumentException with code {@code GEMINI_GUARDRAIL_VIOLATION}
     *         if Gemini detects a deletion operation
     * @throws IllegalArgumentException with code {@code GEMINI_PARSE_FAILURE}
     *         if the response cannot be parsed as valid JSON
     * @throws IllegalStateException with code {@code GEMINI_API_UNAVAILABLE}
     *         if the Gemini API returns a non-2xx HTTP status
     */
    public GeminiResult generateInfrastructureConfig(String userPrompt) {
        long startTimeMs = System.currentTimeMillis();

        String fullPrompt = SYSTEM_GUARDRAIL + userPrompt.trim();
        Map<String, Object> requestBody = buildRequestBody(fullPrompt);

        log.info("[GeminiClientService] Sending prompt to Gemini API. Prompt length: {} chars",
            fullPrompt.length());

        GeminiResponse geminiResponse;
        try {
            geminiResponse = restClient.post()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("key", apiKey)
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException("GEMINI_API_UNAVAILABLE");
                })
                .body(GeminiResponse.class);

        } catch (IllegalStateException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("[GeminiClientService] RestClient error calling Gemini: {}", e.getMessage());
            throw new IllegalStateException("GEMINI_API_UNAVAILABLE");
        }

        long latencyMs = System.currentTimeMillis() - startTimeMs;

        if (geminiResponse == null) {
            throw new IllegalStateException("GEMINI_API_UNAVAILABLE");
        }

        String rawText = geminiResponse.extractText();
        if (rawText == null || rawText.isBlank()) {
            log.error("[GeminiClientService] Gemini returned an empty text content block.");
            throw new IllegalArgumentException("GEMINI_PARSE_FAILURE");
        }

        String cleanedJson = stripMarkdownFences(rawText.trim());

        validateAndCheckGuardrail(cleanedJson);

        GeminiResponse.UsageMetadata usage = geminiResponse.getUsageMetadata();
        Integer totalTokens  = usage != null ? usage.getTotalTokenCount()  : null;
        Integer promptTokens = usage != null ? usage.getPromptTokenCount() : null;

        log.info("[GeminiClientService] Gemini response received. Latency: {}ms, Tokens: {}",
            latencyMs, totalTokens);

        return new GeminiResult(
            cleanedJson,
            fullPrompt,
            rawText,
            totalTokens,
            promptTokens,
            (int) latencyMs
        );
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Builds the Gemini API request payload.
     * Uses the {@code generationConfig} to enforce deterministic, low-temperature
     * output suitable for structured JSON generation.
     */
    private Map<String, Object> buildRequestBody(String fullPrompt) {
        return Map.of(
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", fullPrompt))
                )
            ),
            "generationConfig", Map.of(
                "temperature",      0.1,   // Low temperature = more deterministic JSON output
                "topP",             0.8,
                "topK",             10,
                "maxOutputTokens",  4096,
                "responseMimeType", "application/json"  // Instructs Gemini to return raw JSON
            ),
            "safetySettings", List.of(
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_ONLY_HIGH")
            )
        );
    }

    /**
     * Strips markdown code fences that some Gemini model versions prepend/append
     * to their output despite explicit instructions to return raw JSON.
     *
     * <p>Handles all common fence variants:
     * <pre>
     *   ```json\n{...}\n```
     *   ```\n{...}\n```
     * </pre>
     * </p>
     */
    private String stripMarkdownFences(String text) {
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }

    /**
     * Parses the cleaned text as JSON and checks for the guardrail violation signal.
     *
     * @throws IllegalArgumentException with code {@code GEMINI_GUARDRAIL_VIOLATION}
     *         if the JSON contains an {@code "error": "FORBIDDEN_OPERATION"} field
     * @throws IllegalArgumentException with code {@code GEMINI_PARSE_FAILURE}
     *         if the text is not valid JSON
     */
    private void validateAndCheckGuardrail(String jsonText) {
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonText);
        } catch (JsonProcessingException e) {
            log.error("[GeminiClientService] Response is not valid JSON: {}", jsonText);
            throw new IllegalArgumentException("GEMINI_PARSE_FAILURE");
        }

        JsonNode errorNode = root.get("error");
        if (errorNode != null && "FORBIDDEN_OPERATION".equals(errorNode.asText())) {
            log.warn("[GeminiClientService] Guardrail triggered — deletion operation detected in prompt.");
            throw new IllegalArgumentException("GEMINI_GUARDRAIL_VIOLATION");
        }

        // Validate that all required fields are present
        String[] requiredFields = {
            "provider", "service", "region", "networkConfig",
            "estimatedMonthlyCostUsd", "costBreakdown", "reasoning", "tags"
        };
        for (String field : requiredFields) {
            if (!root.has(field)) {
                log.error("[GeminiClientService] Required field '{}' missing from Gemini response: {}",
                    field, jsonText);
                throw new IllegalArgumentException("GEMINI_PARSE_FAILURE");
            }
        }
    }

    // ── Result value object ──────────────────────────────────────────────────

    /**
     * Immutable value object carrying the full result of a Gemini API call.
     * Carries both the processed config and the raw data needed for compliance logging.
     */
    public record GeminiResult(
        String validatedConfigJson,
        String sanitizedPromptSent,
        String rawGeminiResponseText,
        Integer totalTokenCount,
        Integer promptTokenCount,
        int     latencyMs
    ) {}
}