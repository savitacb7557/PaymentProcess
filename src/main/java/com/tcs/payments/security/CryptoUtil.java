package com.tcs.payments.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CryptoUtil is responsible for holding an AES-256 key and providing helper methods
 * to construct it correctly from Base64, Hex, or raw bytes.
 *
 * It enforces that the key material is EXACTLY 32 bytes (256 bits).
 */
public final class CryptoUtil {

    private final SecretKeySpec keySpec;

    /**
     * Construct with raw key bytes. Must be exactly 32 bytes for AES-256.
     */
    public CryptoUtil(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }
    

    /**
        * Encrypts the given plaintext bytes using AES-GCM and returns the concatenation: IV || ciphertext.
        * You can Base64-encode it at the call site.
        */
       public byte[] encrypt(byte[] plaintext) throws Exception {
           byte[] iv = new byte[12]; // 96-bit IV for GCM
           new SecureRandom().nextBytes(iv);

           Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
           GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
           cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

           byte[] ciphertextWithTag = cipher.doFinal(plaintext);

           // Return iv || ciphertextWithTag so decrypt can recover IV
           byte[] out = new byte[iv.length + ciphertextWithTag.length];
           System.arraycopy(iv, 0, out, 0, iv.length);
           System.arraycopy(ciphertextWithTag, 0, out, iv.length, ciphertextWithTag.length);
           return out;
       }

       /**
        * Convenience overload: encrypt a String and return Base64 of (IV || ciphertext).
        */
       public String encryptToBase64(String plaintext) throws Exception {
           byte[] out = encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
           return Base64.getEncoder().encodeToString(out);
       }


    /**
     * Construct from a Base64-encoded string that represents 32 raw bytes.
     * Trims the input to avoid issues with trailing newlines/spaces.
     */
    public static CryptoUtil fromBase64(String base64Key) {
        if (base64Key == null || base64Key.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 AES key must be provided");
        }
        byte[] decoded = Base64.getDecoder().decode(base64Key.trim());
        return new CryptoUtil(decoded);
    }

    /**
     * Construct from a hex-encoded string (64 hex characters representing 32 bytes).
     * Trims input and validates even-length hex.
     */
    public static CryptoUtil fromHex(String hexKey) {
        if (hexKey == null || hexKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Hex AES key must be provided");
        }
        byte[] decoded = decodeHex(hexKey.trim());
        return new CryptoUtil(decoded);
    }


    /**
     * Construct from a plain ASCII string. ONLY works if it is exactly 32 ASCII characters.
     * (Not recommended for production; prefer random Base64/Hex key material.)
     */
    public static CryptoUtil fromPlainKey(String plainKey) {
        if (plainKey == null || plainKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain AES key must be provided");
        }
        String trimmed = plainKey.trim();
        byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        // This encodes the literal characters as bytes; must be exactly 32 bytes
        return new CryptoUtil(bytes);
    }

    /**
     * Accessor for the SecretKeySpec if you need to use it with Cipher, etc.
     */
    public SecretKeySpec getKeySpec() {
        return this.keySpec;
    }

    /**
     * Minimal hex decoder without external dependencies.
     */
    private static byte[] decodeHex(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex key length must be even");
        }
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character in AES key");
            }
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }
}
