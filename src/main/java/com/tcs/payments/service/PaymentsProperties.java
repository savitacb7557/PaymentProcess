package com.tcs.payments.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;
import java.util.ArrayList;

@ConfigurationProperties(prefix = "payments")
public class PaymentsProperties {

    private Stripe stripe = new Stripe();
    private Razorpay razorpay = new Razorpay();
    private Security security = new Security();
    // NEW: Add this list to match the YAML "supported-regions"
    private List<RegionConfig> supportedRegions = new ArrayList<>();

    public Stripe getStripe() { return stripe; }
    public Razorpay getRazorpay() { return razorpay; }
    public Security getSecurity() { return security; }
    
    // NEW: Getter and Setter for the regions list
    public List<RegionConfig> getSupportedRegions() { return supportedRegions; }
    public void setSupportedRegions(List<RegionConfig> supportedRegions) { this.supportedRegions = supportedRegions; }

    // NEW: Inner class to hold region details
    public static class RegionConfig {
        private String code;
        private String currency;
        private String provider;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
    }

    public static class Stripe {
        private String secretKey;
        private String webhookSecret;
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }

    public static class Razorpay {
        private String keyId;
        private String keySecret;
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKeySecret() { return keySecret; }
        public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
    }

    public static class Security {
        private String pciMetaKeyBase64;
        public String getPciMetaKeyBase64() { return pciMetaKeyBase64; }
        public void setPciMetaKeyBase64(String pciMetaKeyBase64) { this.pciMetaKeyBase64 = pciMetaKeyBase64; }
    }
}