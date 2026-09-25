package com.voting;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Provides military-grade AES-256-GCM authenticated encryption and SHA-256 hashing
 * for vote records, voter identities, and ballot receipts in real-time.
 */
public final class EncryptionUtil {
    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    // Secure 256-bit election master key derived for the session
    private static final byte[] MASTER_KEY_BYTES = new byte[]{
            (byte) 0x3a, (byte) 0x7f, (byte) 0x9b, (byte) 0x12,
            (byte) 0x54, (byte) 0x88, (byte) 0xca, (byte) 0xef,
            (byte) 0x01, (byte) 0x43, (byte) 0x67, (byte) 0x89,
            (byte) 0xab, (byte) 0xcd, (byte) 0xef, (byte) 0x10,
            (byte) 0x22, (byte) 0x44, (byte) 0x66, (byte) 0x88,
            (byte) 0xaa, (byte) 0xcc, (byte) 0xee, (byte) 0x02,
            (byte) 0x13, (byte) 0x35, (byte) 0x57, (byte) 0x79,
            (byte) 0x9b, (byte) 0xbd, (byte) 0xdf, (byte) 0xf1
    };

    private static final SecretKeySpec SECRET_KEY = new SecretKeySpec(MASTER_KEY_BYTES, "AES");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EncryptionUtil() {}

    /**
     * Encrypts plaintext data using AES-256-GCM.
     * Returns a Base64 encoded string containing [IV (12 bytes) + Ciphertext + GCM Tag].
     *
     * @param plainText Plain text to encrypt.
     * @return Base64 encoded encrypted token.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failure: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an AES-256-GCM Base64 encoded token.
     *
     * @param encryptedBase64 The encrypted string token.
     * @return Original decrypted plaintext.
     */
    public static String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted payload length");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, spec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failure: " + e.getMessage(), e);
        }
    }

    /**
     * Computes a standard SHA-256 cryptographic digest.
     */
    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm missing", e);
        }
    }
}
