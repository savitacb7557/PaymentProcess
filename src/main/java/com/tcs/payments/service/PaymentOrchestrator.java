package com.tcs.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.payments.model.Amount;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;
import com.tcs.payments.provider.ApplePayProviderStripe;
import com.tcs.payments.provider.CardProviderStripe;
import com.tcs.payments.provider.CodProvider;
import com.tcs.payments.provider.EmiProviderRazorpay;
import com.tcs.payments.provider.GooglePayProviderStripe;
import com.tcs.payments.risk.FraudEngine;
import com.tcs.payments.security.CryptoUtil;
import com.tcs.payments.security.ThreeDSDecider;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

    private final CardProviderStripe card;
    private final ApplePayProviderStripe apple;
    private final GooglePayProviderStripe gpay;
    private final CodProvider cod = new CodProvider();
    private final EmiProviderRazorpay emi;
    private final FraudEngine fraud = new FraudEngine();
    private final ThreeDSDecider decider = new ThreeDSDecider();
    private final CryptoUtil cryptoUtil;
    private final PaymentsProperties paymentsProperties;

    public PaymentOrchestrator(PaymentsProperties paymentsProperties) throws Exception {
        this.paymentsProperties = paymentsProperties;

        // Initialize Providers using injected properties
        String stripeKey = paymentsProperties.getStripe().getSecretKey();
        this.card = new CardProviderStripe(stripeKey);
        this.apple = new ApplePayProviderStripe(stripeKey);
        this.gpay = new GooglePayProviderStripe(stripeKey);
        
        this.emi = new EmiProviderRazorpay(
            paymentsProperties.getRazorpay().getKeyId(),
            paymentsProperties.getRazorpay().getKeySecret()
        );

        // Initialize Crypto logic
        byte[] keyBytes;
        String pciKey = paymentsProperties.getSecurity().getPciMetaKeyBase64();
        
        if (pciKey == null || pciKey.isBlank()) {
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("Using ephemeral PCI key; data will not be decryptable after restart.");
        } else {
            keyBytes = Base64.getDecoder().decode(pciKey);
        }

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("pciMetaKeyBase64 must be 32 bytes (AES-256).");
        }
        this.cryptoUtil = new CryptoUtil(keyBytes);
    }

    public PaymentResponse initiate(PaymentRequest req) throws Exception {
        // --- FIXED POLICY CHECK ---
        // Dynamically find the region from the loaded YAML configuration
    	PaymentsProperties.RegionConfig regionConfig = paymentsProperties.getSupportedRegions().stream()
    	        .filter(r -> r.getCode().equalsIgnoreCase(req.getRegion().name())) 
    	        .findFirst()
    	        .orElse(null);

        if (regionConfig == null) {
            log.error("Validation failed: Region {} not found in configuration", req.getRegion());
            return fail("Policy", "region_not_supported");
        }

        // Logic for Coupon/Amount
        CouponsService.Result cr = new CouponsService()
                .apply(req.getRegion(), req.getAmount(), req.getCouponCode());
        Amount toCharge = cr.finalAmount();

        // Fraud and Risk evaluation
        FraudEngine.Verdict verdict = fraud.evaluate(req);
        if (verdict.action == FraudEngine.Action.BLOCK) {
            return fail("Risk", "blocked:" + String.join(",", verdict.reasons));
        }

        ThreeDSDecider.Decision d = decider.decide(ThreeDSDecider.Pref.RISK_BASED, verdict.action);
        card.setRequest3ds(d.request3ds);

        // Metadata Encryption
        Map<String, String> md = req.getMetadata() == null ? new HashMap<>() : new HashMap<>(req.getMetadata());
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"reasons\":" + mapper.writeValueAsString(verdict.reasons)
                    + ",\"threeDS\":" + mapper.writeValueAsString(d) + "}";

        String encMeta = Base64.getEncoder().encodeToString(
            cryptoUtil.encrypt(json.getBytes(StandardCharsets.UTF_8))
        );

        md.put("encMeta", encMeta);
        req.setMetadata(md);
        req.setAmount(toCharge);

        // Routing to correct provider
        switch (req.getMethod()) {
            case CARD:
                if (req.getCardToken() == null || req.getCardToken().isBlank())
                    return fail("Card", "missing_card_token");
                return card.authorize(req);

            case APPLE_PAY:
                if (req.getWalletToken() == null || req.getWalletToken().isBlank())
                    return fail("ApplePay", "missing_wallet_token");
                return apple.authorize(req);

            case GOOGLE_PAY:
                if (req.getWalletToken() == null || req.getWalletToken().isBlank())
                    return fail("GooglePay", "missing_wallet_token");
                return gpay.authorize(req);

            case EMI:
                if (req.getEmiPlan() == null) return fail("EMI", "missing_emi_plan");
                // Simplified EMI check based on provider name in YAML
                if (!"razorpay".equalsIgnoreCase(regionConfig.getProvider())) {
                    return fail("EMI", "emi_not_allowed_for_region");
                }
                return emi.authorize(req);

            case COD:
                return cod.authorize(req);

            default:
                return fail("Unknown", "unsupported_method");
        }
    }

    public PaymentResponse capture(String provider, String intentId) throws Exception {
        switch (provider) {
            case "Stripe": return card.capture(intentId);
            case "COD": return cod.capture(intentId);
            case "Razorpay": return PaymentResponse.of(PaymentResponse.Status.AUTHORIZED, provider);
            default: return fail(provider, "capture_not_supported_for_provider");
        }
    }

    //this method added for refund option
    public PaymentResponse refund(String provider, String paymentId, Integer amount) throws Exception {
        switch (provider.toLowerCase()) {
            case "stripe":
                // Assuming card is your CardProviderStripe instance
                return card.refund(paymentId, amount); 
                
            case "razorpay":
                // Call the emi (EmiProviderRazorpay) instance
                return emi.refund(paymentId, amount);

            case "cod":
                return fail("COD", "refund_not_supported_for_cod");

            default:
                return fail(provider, "unknown_provider");
        }
    }
    
    private PaymentResponse fail(String provider, String reason) {
        PaymentResponse r = new PaymentResponse();
        r.setStatus(PaymentResponse.Status.FAILED);
        r.setProvider(provider);
        r.setFailureReason(reason);
        return r;
    }
}