package com.tcs.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.payments.model.Amount;
import com.stripe.service.CouponService;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;
import com.tcs.payments.policy.RegionPolicy;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



@Service
public class PaymentOrchestrator {
	private final CardProviderStripe card;
	private final ApplePayProviderStripe apple;
	private final GooglePayProviderStripe gpay;
	private final CodProvider cod = new CodProvider();
	private final EmiProviderRazorpay emi;
	private final FraudEngine fraud = new FraudEngine();
	private final ThreeDSDecider decider = new ThreeDSDecider();
	private final CryptoUtil cryptoUtil;
	
	public PaymentOrchestrator(
			@Value("${payments.stripe.secretKey}") String stripeKey,
			@Value("${payments.razorpay.keyId}") String razorpayKeyId,
			@Value("${payments.razorpay.keySecret}") String razorpayKeySecret,
			@Value("${payments.security.pciMetaKeyBase64:}") String pciKey
			) throws Exception {
		this.card = new CardProviderStripe(stripeKey);
		this.apple = new ApplePayProviderStripe(stripeKey);
		this.gpay = new GooglePayProviderStripe(stripeKey);
		this.emi = new EmiProviderRazorpay(razorpayKeyId,razorpayKeySecret);
		/*
		 * this.cryptoUtil = new CryptoUtil(pciKey == null ?
		 * java.util.Base64.getEncoder().encodeToString(new byte[32]) : pciKey);
		 */

		// Prepare 32-byte AES key material
		        byte[] keyBytes;
		        if (pciKey == null || pciKey.isBlank()) {
		            // Fallback: securely generate a 256-bit key.
		            keyBytes = new byte[32];
		            new SecureRandom().nextBytes(keyBytes);
		            // NOTE: Prefer configuring a stable key in your env to avoid decryption issues across restarts.
		        } else {
		            // Expect base64-encoded key; decode to raw bytes.
		            keyBytes = Base64.getDecoder().decode(pciKey);
		        }

		        if (keyBytes.length != 32) {
		            throw new IllegalArgumentException(
		                "payments.security.pciMetaKeyBase64 must decode to 32 bytes (AES-256)."
		            );
		        }

		        this.cryptoUtil = new CryptoUtil(keyBytes);
		    }

	
	public PaymentResponse initiate(PaymentRequest req) throws Exception {
		RegionPolicy.Policy policy = RegionPolicy.POLICIES.get(req.getRegion());
				if(policy == null) return fail("Policy", "region_not_supported");
				if(!policy.enabledMethods.contains(req.getMethod())) return
						fail("Policy","method_not_enabled_for_region");
				

CouponsService.Result cr = new CouponsService()
    .apply(req.getRegion(), req.getAmount(), req.getCouponCode());
				Amount toCharge = cr.finalAmount();
				
				FraudEngine.Verdict verdict = fraud.evaluate(req);
				if(verdict.action == FraudEngine.Action.BLOCK)return fail("Risk",
						"blocked:"+String.join(",", verdict.reasons));
				
				ThreeDSDecider.Decision d = decider.decide(ThreeDSDecider.Pref.RISK_BASED, verdict.action);
				card.setRequest3ds(d.request3ds);
				
				java.util.Map<String,String> md = req.getMetadata() == null ?
						new java.util.HashMap<>(): new java.util.HashMap<>(req.getMetadata());
				/*String encMeta = cryptoUtil.encrypt("{"reasons":+new
						com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(verdict.reasons)+","threeDS":"+new
						com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(d)+"}").data;*/



ObjectMapper mapper = new ObjectMapper();

String json = "{\"reasons\":" + mapper.writeValueAsString(verdict.reasons)
            + ",\"threeDS\":" + mapper.writeValueAsString(d) + "}";

// Option 1: encrypt -> bytes, then Base64
String encMeta = Base64.getEncoder().encodeToString(
    cryptoUtil.encrypt(json.getBytes(StandardCharsets.UTF_8))
);


				md.put("encMeta", encMeta);
				req.setMetadata(md);
				req.setAmount(toCharge);
				
				

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
				                // Fix HTML entity && and use instance 'policy' (not type 'Policy')
				                if (!policy.emi.enabled ||
				                    (policy.emi.allowedTenures != null &&
				                     !policy.emi.allowedTenures.contains(req.getEmiPlan().getTenureMonths()))) {
				                    return fail("EMI", "emi_not_allowed_for_region_or_tenure");
				                }
				                return emi.authorize(req);

				            case COD:
				                return cod.authorize(req);

				            default:
				                return fail("Unknown", "unsupported_method");
				        }
				    }

public PaymentResponse capture(String provider, String intentId) throws Exception {
	switch(provider) {
	case "Stripe": return card.capture(intentId);
	case "COD": return cod.capture(intentId);
	case "Razorpay": return 
			PaymentResponse.of(PaymentResponse.Status.AUTHORIZED, provider);
	default: return fail(provider,"capture_not_supported_for_provider");
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

    



