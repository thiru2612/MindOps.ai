package com.ai.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an attempt is made to delete a {@code CloudCredential} that
 * is actively referenced by a deployment plan in EXECUTING or DESTROYING status.
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CredentialInUseException extends RuntimeException {

    public CredentialInUseException(String credentialPublicId) {
        super("Credential " + credentialPublicId + " is actively used by a running deployment " +
              "and cannot be deleted until the deployment completes or fails.");
    }
}