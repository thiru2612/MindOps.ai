package com.ai.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AwsCredentialRequest {

    @NotBlank(message = "Credential label is required.")
    @Size(max = 100, message = "Credential label must not exceed 100 characters.")
    private String credentialLabel;

    /**
     * AWS Access Key IDs always begin with a 4-character type prefix (e.g. AKIA, ASIA)
     * followed by 16 uppercase alphanumeric characters — total 20 characters.
     */
    @NotBlank(message = "AWS Access Key ID is required.")
    @Pattern(
        regexp  = "^(AKIA|ASIA|AROA|AIDA|ANPA|ANVA|APKA)[A-Z0-9]{16}$",
        message = "AWS Access Key ID format is invalid. Expected format: AKIA... (20 characters)."
    )
    private String accessKeyId;

    @NotBlank(message = "AWS Secret Access Key is required.")
    @Size(min = 40, max = 44, message = "AWS Secret Access Key must be 40 characters.")
    private String secretAccessKey;

    @NotBlank(message = "Default AWS region is required.")
    @Pattern(
        regexp  = "^[a-z]{2}-[a-z]+-[0-9]{1}$",
        message = "Region format is invalid. Expected format: e.g. us-east-1, ap-south-1."
    )
    private String defaultRegion;
}