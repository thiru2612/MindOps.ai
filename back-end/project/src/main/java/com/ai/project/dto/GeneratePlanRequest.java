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
public class GeneratePlanRequest {

    @NotBlank(message = "A natural language prompt is required.")
    @Size(
        min     = 10,
        max     = 2000,
        message = "Prompt must be between 10 and 2000 characters."
    )
    private String prompt;

    @NotBlank(message = "A credential ID is required to associate with this deployment plan.")
    @Pattern(
        regexp  = "^cred_[a-zA-Z0-9]{12}$",
        message = "Credential ID format is invalid. Expected format: cred_xxxxxxxxxxxx."
    )
    private String credentialId;
}