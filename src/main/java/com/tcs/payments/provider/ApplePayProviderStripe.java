package com.tcs.payments.provider;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;
import java.util.HashMap;

public class ApplePayProviderStripe implements PaymentProvider {
    
    public ApplePayProviderStripe(String secretKey) {
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentResponse authorize(PaymentRequest req) throws Exception {
        // 1. Build parameters with 2025 Mandatory return_url and Manual Capture
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(req.getAmount().getValue())
            .setCurrency(req.getAmount().getCurrency().name().toLowerCase()) 
            .setPaymentMethod(req.getWalletToken())            
            .setConfirm(true)
            // FIX 1: Mandatory for 2025 redirect-based authentication
            .setReturnUrl(req.getReturnUrl() != null ? req.getReturnUrl() : "http://localhost:3000/success")
            // FIX 2: Set to MANUAL so your /capture endpoint can be tested
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
            .putAllMetadata(req.getMetadata() == null ? new HashMap<>() : req.getMetadata())
            .build();

        // 2. Build RequestOptions for Idempotency
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(req.getIdempotencyKey())
            .build();

        // 3. Create the Intent
        PaymentIntent intent = PaymentIntent.create(params, requestOptions);

        // 4. Map the response
        PaymentResponse r = new PaymentResponse();
        r.setProvider("Stripe");
        r.setIntentId(intent.getId());
        r.setClientSecret(intent.getClientSecret());

        switch (intent.getStatus()) {
            case "requires_action":
                r.setStatus(PaymentResponse.Status.REQUIRES_ACTION);
                break;
            case "requires_capture":
                // This is the success state for Manual Capture
                r.setStatus(PaymentResponse.Status.AUTHORIZED);
                break;
            case "requires_payment_method":
                r.setStatus(PaymentResponse.Status.FAILED);
                r.setFailureReason("requires_payment_method");
                break;
            default:
                r.setStatus(PaymentResponse.Status.AUTHORIZED);
                break;
        }
        return r;
    }

    @Override
    public PaymentResponse capture(String intentId) throws Exception {
        // Capture logic for Wallet payments is identical to Card
        PaymentIntent intent = PaymentIntent.retrieve(intentId);
        intent = intent.capture();
        
        PaymentResponse r = new PaymentResponse();
        r.setStatus(PaymentResponse.Status.CAPTURED);
        r.setProvider("Stripe");
        r.setIntentId(intent.getId());
        return r;
    }
    
    //added for refund option
    @Override
    public PaymentResponse refund(String intentId, Integer amount) throws Exception {
        try {
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(intentId);

            if (amount != null && amount > 0) {
                builder.setAmount(Long.valueOf(amount));
            }

            // 1. Execute the refund on Stripe
            Refund stripeRefund = Refund.create(builder.build());

            PaymentResponse r = new PaymentResponse();
            
            // 2. KEEP the provider name as "Stripe"
            r.setProvider("Stripe"); 
            
            // 3. STORE the Refund ID in intentId since setProviderReference is missing
            r.setIntentId(stripeRefund.getId()); 
            
            if ("succeeded".equals(stripeRefund.getStatus())) {
                // Change SUCCESS to CAPTURED
                r.setStatus(PaymentResponse.Status.CAPTURED); 
            } else {
                r.setStatus(PaymentResponse.Status.FAILED);
                r.setFailureReason("Refund status: " + stripeRefund.getStatus());
            }
            
            return r;

        } catch (Exception e) {
            PaymentResponse r = new PaymentResponse();
            r.setStatus(PaymentResponse.Status.FAILED);
            r.setProvider("Stripe");
            r.setFailureReason(e.getMessage());
            return r;
        }
    }
}