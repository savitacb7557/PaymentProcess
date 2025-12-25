package com.tcs.payments.provider;

import java.util.HashMap;
import java.util.Map;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class CardProviderStripe implements PaymentProvider { 
    
    public CardProviderStripe(String secretKey) {
        Stripe.apiKey = secretKey;
    }
    
    private Boolean request3ds = false;
    
    public void setRequest3ds(Boolean b) {
        this.request3ds = b; 
    }
    
    @Override
    public PaymentResponse authorize(PaymentRequest req) throws Exception {
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(req.getAmount().getValue())
                .setCurrency(req.getAmount().getCurrency().name().toLowerCase())
                .setPaymentMethod(req.getCardToken())
                .setConfirm(true)
                // MANDATORY for 2025: return_url for redirect/3DS support
                .setReturnUrl(req.getReturnUrl() != null ? req.getReturnUrl() : "http://localhost:3000/success")
                // FIX: Set to MANUAL so the /capture endpoint can be tested later
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .putAllMetadata(req.getMetadata() == null ? new HashMap<>() : req.getMetadata());

        // Configure 3D Secure
        PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure request3dsType = 
                request3ds ? PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY :
                             PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.AUTOMATIC;

        builder.setPaymentMethodOptions(
            PaymentIntentCreateParams.PaymentMethodOptions.builder()
                .setCard(PaymentIntentCreateParams.PaymentMethodOptions.Card.builder()
                    .setRequestThreeDSecure(request3dsType)
                    .build())
                .build()
        );

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(req.getIdempotencyKey())
                .build();

        PaymentIntent intent = PaymentIntent.create(builder.build(), options);

        PaymentResponse r = new PaymentResponse();
        r.setProvider("Stripe");
        r.setIntentId(intent.getId());
        r.setClientSecret(intent.getClientSecret());

        // Status mapping for Authorize-only flow
        switch (intent.getStatus()) {
            case "requires_action":
                r.setStatus(PaymentResponse.Status.REQUIRES_ACTION);
                break;
            case "requires_capture":
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
        // Retrieve the intent to ensure it's in the correct state
        PaymentIntent intent = PaymentIntent.retrieve(intentId);
        
        // Capture the authorized funds
        intent = intent.capture();

        // Expand to get the receipt URL from the latest charge
        Map<String, Object> expandParams = new HashMap<>();
        expandParams.put("expand", java.util.List.of("latest_charge"));
        intent = PaymentIntent.retrieve(intentId, expandParams, null);
        
        Charge latestCharge = (Charge) intent.getLatestChargeObject();
        String receiptUrl = latestCharge != null ? latestCharge.getReceiptUrl() : null;

        PaymentResponse r = new PaymentResponse();
        r.setStatus(PaymentResponse.Status.CAPTURED);
        r.setProvider("Stripe");
        r.setIntentId(intent.getId());
        r.setReceiptUrl(receiptUrl);
        return r;
    }
    
    @Override
    public PaymentResponse refund(String intentId, Integer amount) throws Exception {
        try {
            // 1. Build refund parameters
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(intentId); // Link to the original PaymentIntent ID

            // 2. Handle Partial vs Full Refund
            if (amount != null && amount > 0) {
                builder.setAmount(Long.valueOf(amount)); // Amount in cents/paise
            }

            // 3. Execute the refund call to Stripe API
            Refund stripeRefund = Refund.create(builder.build());

            // 4. Map the response
            PaymentResponse r = new PaymentResponse();
            r.setProvider("Stripe");
            
            // Store the Refund ID in the existing intentId field if setProviderReference is missing
            r.setIntentId(stripeRefund.getId()); 
            
            if ("succeeded".equals(stripeRefund.getStatus())) {
                // Use CAPTURED or whichever success status is in your Enum
                r.setStatus(PaymentResponse.Status.CAPTURED); 
            } else {
                r.setStatus(PaymentResponse.Status.FAILED);
                r.setFailureReason("Refund status: " + stripeRefund.getStatus());
            }
            return r;

        } catch (Exception e) {
            // Handle Stripe errors (e.g., already refunded, insufficient funds)
            PaymentResponse r = new PaymentResponse();
            r.setStatus(PaymentResponse.Status.FAILED);
            r.setProvider("Stripe");
            r.setFailureReason(e.getMessage());
            return r;
        }
    }
}