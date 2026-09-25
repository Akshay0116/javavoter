package com.voting;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

/**
 * Implements RFC 6238 (TOTP: Time-Based One-Time Password Algorithm) and RFC 4226 (HOTP).
 * Compatible with Google Authenticator, Microsoft Authenticator, Authy, Apple Passwords, etc.
 * Pure Java SE implementation with zero external dependencies.
 */
public final class TotpUtil {
    public static final String DEFAULT_ADMIN_SECRET = "JBSWY3DPEHPK3PXP"; // Base32 Secret Key
    public static final String ISSUER = "SecureVote";
    public static final String ACCOUNT = "admin";
    private static final int TIME_STEP_SECONDS = 30;

    private TotpUtil() {}

    /**
     * Generates standard otpauth URI for QR code generation.
     */
    public static String getOtpAuthUri(String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                ISSUER, ACCOUNT, secret, ISSUER);
    }

    /**
     * Generates a QR Code image URL using a standard public QR renderer.
     */
    public static String getQrCodeUrl(String otpAuthUri) {
        return "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=" +
                java.net.URLEncoder.encode(otpAuthUri, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Verifies a 6-digit code against the secret key.
     * Allows a clock drift window of [-1, 0, +1] time steps (approx +/- 30 seconds).
     *
     * @param base32Secret Secret key in Base32.
     * @param code         6-digit user submitted code.
     * @return true if valid, false otherwise.
     */
    public static boolean verifyCode(String base32Secret, String code) {
        if (code == null || code.trim().length() != 6) {
            return false;
        }

        try {
            int inputCode = Integer.parseInt(code.trim());
            long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

            // Check current window and adjacent windows to tolerate clock skew
            for (int offset = -1; offset <= 1; offset++) {
                int expectedCode = generateTotp(base32Secret, currentWindow + offset);
                if (expectedCode == inputCode) {
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    /**
     * Computes the 6-digit TOTP code for a specific time window counter.
     */
    public static int generateTotp(String base32Secret, long timeWindow) {
        byte[] keyBytes = decodeBase32(base32Secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "RAW"));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation (RFC 4226)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % 1000000;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("HMAC-SHA1 algorithm not available", e);
        }
    }

    /**
     * Minimal RFC 4648 Base32 decoder.
     */
    private static byte[] decodeBase32(String base32) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = base32Chars.indexOf(c);
            if (val < 0) continue;

            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[next++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }

    /**
     * CLI utility to print the current real-time TOTP code and time remaining.
     */
    public static void main(String[] args) {
        long epochSec = System.currentTimeMillis() / 1000L;
        long timeWindow = epochSec / TIME_STEP_SECONDS;
        int remainingSec = (int) (TIME_STEP_SECONDS - (epochSec % TIME_STEP_SECONDS));
        int code = generateTotp(DEFAULT_ADMIN_SECRET, timeWindow);

        System.out.printf("Current Authenticator Code: %06d (%d seconds remaining)%n", code, remainingSec);
        System.out.println("Secret Key (Base32)       : " + DEFAULT_ADMIN_SECRET);
        System.out.println("OTP Auth URI              : " + getOtpAuthUri(DEFAULT_ADMIN_SECRET));
    }
}
