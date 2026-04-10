package com.ai.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Internal DTO mapping the Gemini API HTTP response envelope.
 *
 * <p>Only the fields required to extract the generated text content are mapped.
 * All other Gemini response fields (safety ratings, citation metadata, etc.)
 * are silently ignored via {@code @JsonIgnoreProperties(ignoreUnknown = true)}.</p>
 *
 * <p>Gemini response structure:
 * <pre>
 * {
 *   "candidates": [
 *     {
 *       "content": {
 *         "parts": [
 *           { "text": "{ ...our JSON payload... }" }
 *         ]
 *       }
 *     }
 *   ],
 *   "usageMetadata": {
 *     "promptTokenCount": 312,
 *     "candidatesTokenCount": 85,
 *     "totalTokenCount": 397
 *   }
 * }
 * </pre>
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    private List<Candidate>   candidates;
    private UsageMetadata     usageMetadata;

    /**
     * Extracts the raw text content from the first candidate's first part.
     * Returns {@code null} if the response structure is incomplete.
     */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) return null;
        Candidate first = candidates.get(0);
        if (first.getContent() == null) return null;
        List<Part> parts = first.getContent().getParts();
        if (parts == null || parts.isEmpty()) return null;
        return parts.get(0).getText();
    }

    // ── Nested mapping classes ────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private Content content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {
        private String text;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsageMetadata {
        @JsonProperty("promptTokenCount")
        private Integer promptTokenCount;

        @JsonProperty("candidatesTokenCount")
        private Integer candidatesTokenCount;

        @JsonProperty("totalTokenCount")
        private Integer totalTokenCount;
    }
}