package com.tcs.payments.security;


import com.tcs.payments.service.PaymentsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Component
public class MetadataEncryptor {
    private static final int AES_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final byte[] keyBytes;
    private final SecureRandom rng = new SecureRandom();
    private final ObjectMapper json = new ObjectMapper();

    public MetadataEncryptor(PaymentsProperties props) {
        String base64 = props.getSecurity().getPciMetaKeyBase64();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Missing payments.security.pciMetaKeyBase64");
        }
        byte[] k = Base64.getDecoder().decode(base64);
        if (k.length != AES_KEY_BYTES) {
            throw new IllegalStateException("AES key must be 32 bytes");
        }
        this.keyBytes = k;
    }

    public String encrypt(Map<String, String> metadata) {
        try {
            byte[] plaintext = json.writeValueAsBytes(metadata);
            byte[] iv = new byte[GCM_IV_BYTES];
            rng.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = c.doFinal(plaintext);

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> decrypt(String base64IvCipher) {
        try {
            byte[] blob = Base64.getDecoder().decode(base64IvCipher);
            if (blob.length <= GCM_IV_BYTES) throw new IllegalStateException("Invalid payload");

            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ct = new byte[blob.length - GCM_IV_BYTES];
            System.arraycopy(blob, 0, iv, 0, iv.length);
            System.arraycopy(blob, iv.length, ct, 0, ct.length);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = c.doFinal(ct);

            Map<?, ?> raw = json.readValue(pt, Map.class);
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                if (!(e.getKey() instanceof String) || !(e.getValue() instanceof String))
                    throw new IllegalStateException("Decrypted metadata must be Map<String,String>");
            }
            return (Map<String, String>) (Map<?, ?>) raw;
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt failed: " + e.getMessage(), e);
        }
    }
}