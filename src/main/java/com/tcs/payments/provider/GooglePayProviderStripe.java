package com.tcs.payments.provider;

import java.util.HashMap;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class GooglePayProviderStripe implements PaymentProvider {
    
    public GooglePayProviderStripe(String secretKey) {
        Stripe.apiKey = secretKey;
    }
    
    @Override
    public PaymentResponse authorize(PaymentRequest req) throws Exception {
        // Build parameters with 2025 Mandatory return_url and Manual Capture
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(req.getAmount().getValue())
            .setCurrency(req.getAmount().getCurrency().name().toLowerCase()) 
            .setPaymentMethod(req.getWalletToken())           
            .setConfirm(true)
            // FIX 1: Mandatory for 2025 redirect-based authentication
            .setReturnUrl(req.getReturnUrl() != null ? req.getReturnUrl() : "http://localhost:3000/success")
            // FIX 2: Set to MANUAL for consistency with your /capture endpoint
            .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
            .putAllMetadata(req.getMetadata() == null ? new HashMap<>() : req.getMetadata());

        // Use modern RequestOptions builder
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(req.getIdempotencyKey())
            .build();

        PaymentIntent intent = PaymentIntent.create(builder.build(), requestOptions);

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
        PaymentIntent intent = PaymentIntent.retrieve(intentId);
        intent = intent.capture();
        
        PaymentResponse r = new PaymentResponse();
        r.setStatus(PaymentResponse.Status.CAPTURED);
        r.setProvider("Stripe");
        r.setIntentId(intent.getId());
        return r;
    }
    
    @Override
    public PaymentResponse refund(String intentId, Integer amount) throws Exception {
        try {
            // 1. Build refund parameters linked to the original PaymentIntent ID (pi_...)
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(intentId); 

            // 2. Handle Partial vs Full Refund (amount is in cents/paise)
            if (amount != null && amount > 0) {
                builder.setAmount(Long.valueOf(amount)); 
            }

            // 3. Execute the refund call to Stripe API
            Refund stripeRefund = Refund.create(builder.build());

            // 4. Map the response
            PaymentResponse r = new PaymentResponse();
            r.setProvider("Stripe");
            
            // Store the Refund ID (re_...) in the existing intentId field
            r.setIntentId(stripeRefund.getId()); 
            
            if ("succeeded".equals(stripeRefund.getStatus())) {
                // Use the correct success status from your PaymentResponse.Status enum
                r.setStatus(PaymentResponse.Status.CAPTURED); 
            } else {
                r.setStatus(PaymentResponse.Status.FAILED);
                r.setFailureReason("Refund status: " + stripeRefund.getStatus());
            }
            return r;

        } catch (Exception e) {
            // Handle Stripe errors (e.g., already refunded, API rate limits)
            PaymentResponse r = new PaymentResponse();
            r.setStatus(PaymentResponse.Status.FAILED);
            r.setProvider("Stripe");
            r.setFailureReason(e.getMessage());
            return r;
        }
    }
}