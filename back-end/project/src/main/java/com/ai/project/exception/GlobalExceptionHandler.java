// package com.ai.project.exception;

// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.authentication.BadCredentialsException;
// import org.springframework.security.authentication.DisabledException;
// import org.springframework.security.authentication.LockedException;
// import org.springframework.validation.FieldError;
// import org.springframework.web.bind.MethodArgumentNotValidException;
// import org.springframework.web.bind.annotation.ExceptionHandler;
// import org.springframework.web.bind.annotation.RestControllerAdvice;
// import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// import java.time.LocalDateTime;
// import java.util.LinkedHashMap;
// import java.util.Map;
// import java.util.stream.Collectors;

// /**
//  * Centralised exception-to-HTTP-response translator for the entire API surface.
//  *
//  * <p>Design contracts:
//  * <ul>
//  *   <li>Every response follows the shape:
//  *       {@code {"error":"ERROR_CODE","message":"Human readable","timestamp":"..."}}</li>
//  *   <li>Validation errors include an additional {@code "fieldErrors"} map keyed by field name.</li>
//  *   <li>Stack traces are NEVER included in any response body. Errors are logged server-side only.</li>
//  *   <li>The catch-all {@link Exception} handler uses a generic message to prevent internal
//  *       implementation details from leaking to the client.</li>
//  * </ul>
//  * </p>
//  */
// @Slf4j
// @RestControllerAdvice
// public class GlobalExceptionHandler {

//     // ── 400 — Validation Errors ──────────────────────────────────────────────

//     /**
//      * Handles bean validation failures from {@code @Valid} on request bodies.
//      * Returns a structured map of field-level errors so the frontend can
//      * display inline field validation messages.
//      */
//     @ExceptionHandler(MethodArgumentNotValidException.class)
//     public ResponseEntity<Map<String, Object>> handleValidationException(
//         MethodArgumentNotValidException ex
//     ) {
//         Map<String, String> fieldErrors = ex.getBindingResult()
//             .getAllErrors()
//             .stream()
//             .filter(error -> error instanceof FieldError)
//             .map(error -> (FieldError) error)
//             .collect(Collectors.toMap(
//                 FieldError::getField,
//                 fieldError -> fieldError.getDefaultMessage() != null
//                     ? fieldError.getDefaultMessage()
//                     : "Invalid value",
//                 // If multiple constraints fail on the same field, keep the first message
//                 (existing, replacement) -> existing
//             ));

//         return ResponseEntity
//             .status(HttpStatus.BAD_REQUEST)
//             .body(buildErrorBody("VALIDATION_FAILED", "One or more fields failed validation.", fieldErrors));
//     }

//     // ── 400 — Illegal Argument / State ───────────────────────────────────────

//     @ExceptionHandler(IllegalArgumentException.class)
//     public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
//         IllegalArgumentException ex
//     ) {
//         log.warn("[GlobalExceptionHandler] IllegalArgumentException: {}", ex.getMessage());
//         return ResponseEntity
//             .status(HttpStatus.BAD_REQUEST)
//             .body(buildErrorBody("BAD_REQUEST", sanitizeMessage(ex.getMessage())));
//     }

//     @ExceptionHandler(IllegalStateException.class)
//     public ResponseEntity<Map<String, Object>> handleIllegalStateException(
//         IllegalStateException ex
//     ) {
//         log.warn("[GlobalExceptionHandler] IllegalStateException: {}", ex.getMessage());
//         return ResponseEntity
//             .status(HttpStatus.BAD_REQUEST)
//             .body(buildErrorBody("INVALID_STATE", sanitizeMessage(ex.getMessage())));
//     }

//     @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//     public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
//         MethodArgumentTypeMismatchException ex
//     ) {
//         String message = String.format(
//             "Parameter '%s' has an invalid value: '%s'.",
//             ex.getName(), ex.getValue()
//         );
//         return ResponseEntity
//             .status(HttpStatus.BAD_REQUEST)
//             .body(buildErrorBody("INVALID_PARAMETER", message));
//     }

//     // ── 401 — Authentication Failures ───────────────────────────────────────

//     @ExceptionHandler(BadCredentialsException.class)
//     public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
//         BadCredentialsException ex
//     ) {
//         return ResponseEntity
//             .status(HttpStatus.UNAUTHORIZED)
//             .body(buildErrorBody("INVALID_CREDENTIALS", "Invalid email or password."));
//     }

//     @ExceptionHandler({DisabledException.class, LockedException.class})
//     public ResponseEntity<Map<String, Object>> handleAccountStatusException(Exception ex) {
//         return ResponseEntity
//             .status(HttpStatus.FORBIDDEN)
//             .body(buildErrorBody("ACCOUNT_DISABLED", "This account has been deactivated. Contact support."));
//     }

//     // ── 403 — Access Denied ──────────────────────────────────────────────────

//     /**
//      * Handles Spring Security's {@link AccessDeniedException}, thrown when an
//      * authenticated user attempts to access a resource they lack authority for
//      * (e.g. a ROLE_USER hitting an ADMIN-only endpoint).
//      */
//     @ExceptionHandler(AccessDeniedException.class)
//     public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
//         AccessDeniedException ex
//     ) {
//         log.warn("[GlobalExceptionHandler] Access denied: {}", ex.getMessage());
//         return ResponseEntity
//             .status(HttpStatus.FORBIDDEN)
//             .body(buildErrorBody(
//                 "ACCESS_DENIED",
//                 "You do not have permission to perform this action."
//             ));
//     }

//     // ── 404 — Resource Not Found ─────────────────────────────────────────────

//     @ExceptionHandler(ResourceNotFoundException.class)
//     public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
//         ResourceNotFoundException ex
//     ) {
//         log.warn("[GlobalExceptionHandler] Resource not found: {}", ex.getMessage());
//         return ResponseEntity
//             .status(HttpStatus.NOT_FOUND)
//             .body(buildErrorBody("RESOURCE_NOT_FOUND", ex.getMessage()));
//     }

//     // ── 409 — Conflict ───────────────────────────────────────────────────────

//     @ExceptionHandler(CredentialInUseException.class)
//     public ResponseEntity<Map<String, Object>> handleCredentialInUseException(
//         CredentialInUseException ex
//     ) {
//         log.warn("[GlobalExceptionHandler] Credential in use: {}", ex.getMessage());
//         return ResponseEntity
//             .status(HttpStatus.CONFLICT)
//             .body(buildErrorBody("CREDENTIAL_IN_USE", ex.getMessage()));
//     }

//     // ── 500 — Catch-All ──────────────────────────────────────────────────────

//     /**
//      * Last-resort handler for any unhandled exception.
//      * Logs the full stack trace server-side for diagnostics but returns only
//      * a generic, non-leaking message to the client.
//      */
//     @ExceptionHandler(Exception.class)
//     public ResponseEntity<Map<String, Object>> handleAllUnhandledExceptions(Exception ex) {
//         log.error("[GlobalExceptionHandler] Unhandled exception: {}", ex.getMessage(), ex);
//         return ResponseEntity
//             .status(HttpStatus.INTERNAL_SERVER_ERROR)
//             .body(buildErrorBody(
//                 "INTERNAL_SERVER_ERROR",
//                 "An unexpected error occurred. Please try again later."
//             ));
//     }

//     // ── Response Builders ────────────────────────────────────────────────────

//     private Map<String, Object> buildErrorBody(String errorCode, String message) {
//         Map<String, Object> body = new LinkedHashMap<>();
//         body.put("error",     errorCode);
//         body.put("message",   message);
//         body.put("timestamp", LocalDateTime.now().toString());
//         return body;
//     }

//     private Map<String, Object> buildErrorBody(
//         String errorCode,
//         String message,
//         Map<String, String> fieldErrors
//     ) {
//         Map<String, Object> body = buildErrorBody(errorCode, message);
//         body.put("fieldErrors", fieldErrors);
//         return body;
//     }

//     /**
//      * Prevents raw internal exception message codes (e.g. "EMAIL_ALREADY_EXISTS")
//      * from being exposed directly as human-readable messages.
//      * Maps known internal codes to user-facing messages.
//      */
//     private String sanitizeMessage(String rawMessage) {
//         if (rawMessage == null) return "An error occurred processing your request.";
//         return switch (rawMessage) {
//             case "EMAIL_ALREADY_EXISTS"         -> "An account with this email already exists.";
//             case "REFRESH_TOKEN_INVALID"        -> "Refresh token is invalid or expired.";
//             case "REFRESH_TOKEN_REVOKED"        -> "Refresh token has been revoked. Please log in again.";
//             case "USER_NOT_FOUND_OR_INACTIVE"   -> "Associated account not found or has been deactivated.";
//             case "CURRENT_PASSWORD_INCORRECT"   -> "The current password provided is incorrect.";
//             case "NEW_PASSWORD_SAME_AS_CURRENT" -> "New password must be different from the current password.";
//             default                             -> rawMessage;
//         };
//     }
// }

package com.ai.project.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception-to-HTTP-response translator for the entire API surface.
 * Updated in Phase 4 to handle {@link RateLimitExceededException} (HTTP 429).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 — Validation Errors ──────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .filter(error -> error instanceof FieldError)
            .map(error -> (FieldError) error)
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "Invalid value",
                (existing, replacement) -> existing
            ));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildErrorBody(
                "VALIDATION_FAILED",
                "One or more fields failed validation.",
                fieldErrors
            ));
    }

    // ── 400 — Illegal Argument / State ───────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
        IllegalArgumentException ex
    ) {
        log.warn("[GlobalExceptionHandler] IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildErrorBody("BAD_REQUEST", sanitizeMessage(ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
        IllegalStateException ex
    ) {
        log.warn("[GlobalExceptionHandler] IllegalStateException: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildErrorBody("INVALID_STATE", sanitizeMessage(ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
        MethodArgumentTypeMismatchException ex
    ) {
        String message = String.format(
            "Parameter '%s' has an invalid value: '%s'.",
            ex.getName(), ex.getValue()
        );
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildErrorBody("INVALID_PARAMETER", message));
    }

    // ── 401 — Authentication Failures ───────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(
        BadCredentialsException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorBody("INVALID_CREDENTIALS", "Invalid email or password."));
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<Map<String, Object>> handleAccountStatusException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(buildErrorBody(
                "ACCOUNT_DISABLED",
                "This account has been deactivated. Contact support."
            ));
    }

    // ── 403 — Access Denied ──────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
        AccessDeniedException ex
    ) {
        log.warn("[GlobalExceptionHandler] Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(buildErrorBody(
                "ACCESS_DENIED",
                "You do not have permission to perform this action."
            ));
    }

    // ── 404 — Resource Not Found ─────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
        ResourceNotFoundException ex
    ) {
        log.warn("[GlobalExceptionHandler] Resource not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(buildErrorBody("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    // ── 409 — Conflict ───────────────────────────────────────────────────────

    @ExceptionHandler(CredentialInUseException.class)
    public ResponseEntity<Map<String, Object>> handleCredentialInUseException(
        CredentialInUseException ex
    ) {
        log.warn("[GlobalExceptionHandler] Credential in use: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(buildErrorBody("CREDENTIAL_IN_USE", ex.getMessage()));
    }

    // ── 429 — Rate Limit Exceeded (NEW in Phase 4) ──────────────────────────

    /**
     * Handles {@link RateLimitExceededException} from the Bucket4j interceptor.
     *
     * <p>Sets both the JSON body and the standard {@code Retry-After} HTTP header
     * (RFC 7231 §7.1.3) so HTTP clients and API gateways can implement automatic
     * back-off without parsing the response body.</p>
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceededException(
        RateLimitExceededException ex
    ) {
        log.warn("[GlobalExceptionHandler] Rate limit exceeded. Retry after {}s.",
            ex.getRetryAfterSeconds());

        Map<String, Object> body = buildErrorBody(
            "RATE_LIMIT_EXCEEDED",
            "You have exceeded the limit of 10 deployment generation requests per hour."
        );
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());

        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
            .body(body);
    }

    // ── 500 — Catch-All ──────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllUnhandledExceptions(Exception ex) {
        log.error("[GlobalExceptionHandler] Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorBody(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later."
            ));
    }

    // ── Response Builders ────────────────────────────────────────────────────

    private Map<String, Object> buildErrorBody(String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error",     errorCode);
        body.put("message",   message);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }

    private Map<String, Object> buildErrorBody(
        String errorCode,
        String message,
        Map<String, String> fieldErrors
    ) {
        Map<String, Object> body = buildErrorBody(errorCode, message);
        body.put("fieldErrors", fieldErrors);
        return body;
    }

    private String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) return "An error occurred processing your request.";
        return switch (rawMessage) {
            case "EMAIL_ALREADY_EXISTS"           -> "An account with this email already exists.";
            case "REFRESH_TOKEN_INVALID"          -> "Refresh token is invalid or expired.";
            case "REFRESH_TOKEN_REVOKED"          -> "Refresh token has been revoked. Please log in again.";
            case "USER_NOT_FOUND_OR_INACTIVE"     -> "Associated account not found or has been deactivated.";
            case "CURRENT_PASSWORD_INCORRECT"     -> "The current password provided is incorrect.";
            case "NEW_PASSWORD_SAME_AS_CURRENT"   -> "New password must be different from the current password.";
            case "PLAN_NOT_PENDING"               -> "This deployment plan cannot be executed because it is not in PENDING status.";
            case "PLAN_OWNERSHIP_VIOLATION"       -> "You do not have permission to execute this deployment plan.";
            case "GEMINI_PARSE_FAILURE"           -> "The AI engine returned an unparseable response. Please rephrase your prompt and try again.";
            case "GEMINI_GUARDRAIL_VIOLATION"     -> "Your request was interpreted as a resource deletion operation, which is not permitted. Use the dedicated teardown endpoint.";
            case "GEMINI_API_UNAVAILABLE"         -> "The AI engine is temporarily unavailable. Please try again in a few moments.";
            default                               -> rawMessage;
        };
    }
}