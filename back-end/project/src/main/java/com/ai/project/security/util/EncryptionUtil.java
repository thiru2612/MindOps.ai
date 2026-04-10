package com.ai.project.security.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256/GCM encryption utility for vaulting cloud credentials at rest.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>AES-GCM provides both confidentiality AND authenticated integrity
 *       (unlike AES-CBC which requires a separate HMAC). Any tampering with
 *       the ciphertext will cause decryption to throw AEADBadTagException.</li>
 *   <li>A fresh 96-bit (12-byte) IV is generated via SecureRandom for every
 *       encrypt() call. Reusing an IV with the same key under GCM is a
 *       catastrophic security failure — this design makes it structurally
 *       impossible.</li>
 *   <li>The IV is prepended to the ciphertext and stored together as a single
 *       colon-delimited Base64 string: "BASE64(IV):BASE64(CIPHERTEXT)".
 *       This is the canonical storage format for all encrypted credential fields.</li>
 *   <li>The master key is injected from the MINDOPS_ENCRYPTION_KEY environment
 *       variable and validated at startup via @PostConstruct. A missing or
 *       incorrectly sized key will prevent the application from starting.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM          = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM      = "AES";
    private static final int    GCM_IV_LENGTH      = 12;   // 96 bits — NIST recommended
    private static final int    GCM_TAG_LENGTH_BITS = 128;  // 128-bit authentication tag
    private static final int    AES_KEY_SIZE_BYTES  = 32;   // 256 bits

    private SecretKey secretKey;

    @Value("${app.encryption.key}")
    private String base64EncodedMasterKey;

    /**
     * Validates and initialises the SecretKey from the environment variable.
     * Called automatically by Spring after dependency injection.
     * Throws IllegalStateException if the key is absent or incorrectly sized —
     * this is intentional: an improperly configured vault must not start silently.
     */
    @PostConstruct
    public void init() {
        if (base64EncodedMasterKey == null || base64EncodedMasterKey.isBlank()) {
            throw new IllegalStateException(
                "[EncryptionUtil] MINDOPS_ENCRYPTION_KEY environment variable is not set. " +
                "Generate one with: openssl rand -base64 32"
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64EncodedMasterKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "[EncryptionUtil] MINDOPS_ENCRYPTION_KEY is not valid Base64.", e
            );
        }

        if (keyBytes.length != AES_KEY_SIZE_BYTES) {
            throw new IllegalStateException(String.format(
                "[EncryptionUtil] MINDOPS_ENCRYPTION_KEY must decode to exactly 32 bytes " +
                "(AES-256). Got %d bytes. Regenerate with: openssl rand -base64 32",
                keyBytes.length
            ));
        }

        this.secretKey = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        log.info("[EncryptionUtil] AES-256/GCM vault initialised successfully.");
    }

    /**
     * Encrypts a plaintext string using AES-256/GCM with a freshly generated IV.
     *
     * @param plaintext the sensitive value to encrypt (e.g. AWS secret key)
     * @return a colon-delimited string "BASE64(IV):BASE64(CIPHERTEXT)" suitable
     *         for direct storage in the database TEXT column
     * @throws EncryptionException if the JVM does not support AES-GCM (should never
     *         occur on any standard JRE 11+)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException(
                "[EncryptionUtil] Cannot encrypt a null or blank value."
            );
        }

        try {
            byte[] iv = generateIv();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String encodedIv         = Base64.getEncoder().encodeToString(iv);
            String encodedCiphertext = Base64.getEncoder().encodeToString(ciphertext);

            return encodedIv + ":" + encodedCiphertext;

        } catch (Exception e) {
            throw new EncryptionException("AES-256/GCM encryption failed.", e);
        }
    }

    /**
     * Decrypts a stored credential value produced by {@link #encrypt(String)}.
     *
     * @param storedValue the "BASE64(IV):BASE64(CIPHERTEXT)" string from the database
     * @return the original plaintext credential value
     * @throws EncryptionException if the stored value is malformed, the key is wrong,
     *         or the GCM authentication tag does not match (tamper detection)
     */
    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            throw new IllegalArgumentException(
                "[EncryptionUtil] Cannot decrypt a null or blank stored value."
            );
        }

        String[] parts = storedValue.split(":", 2);
        if (parts.length != 2) {
            throw new EncryptionException(
                "Stored credential value is malformed. Expected format: 'BASE64(IV):BASE64(CIPHERTEXT)'."
            );
        }

        try {
            byte[] iv         = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintextBytes = cipher.doFinal(ciphertext);

            return new String(plaintextBytes, java.nio.charset.StandardCharsets.UTF_8);

        } catch (javax.crypto.AEADBadTagException e) {
            // Specific catch for GCM authentication tag failure — indicates tampering
            throw new EncryptionException(
                "AES-GCM authentication tag verification failed. " +
                "The stored credential may have been tampered with.", e
            );
        } catch (Exception e) {
            throw new EncryptionException("AES-256/GCM decryption failed.", e);
        }
    }

    /**
     * Generates a cryptographically secure random 96-bit (12-byte) IV.
     * Must be unique per encryption operation. Never reused.
     */
    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ── Custom Exception ─────────────────────────────────────────────────────

    /**
     * Unchecked exception wrapping all encryption/decryption failures.
     * Allows callers to handle vault errors uniformly without checked exception boilerplate.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message) {
            super(message);
        }
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}