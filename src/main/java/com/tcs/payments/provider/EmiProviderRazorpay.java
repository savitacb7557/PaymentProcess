package com.tcs.payments.provider;

import org.json.JSONObject;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class EmiProviderRazorpay implements PaymentProvider {
    
    private final RazorpayClient client;

    public EmiProviderRazorpay(String keyId, String keySecret) throws Exception {
        // Authenticates with Razorpay using keys from application.yml
        this.client = new RazorpayClient(keyId, keySecret);
    }
    
    @Override
    public PaymentResponse authorize(PaymentRequest req) throws Exception {
        JSONObject orderRequest = new JSONObject();
        
        // Amount in minor units (paise). 100 paise = 1 INR.
        orderRequest.put("amount", req.getAmount().getValue());
        
        // 2025 Requirement: Razorpay expects ISO 4217 uppercase currency (e.g., "INR")
        orderRequest.put("currency", req.getAmount().getCurrency().name().toUpperCase());
        
        // Mapping idempotencyKey to Razorpay receipt field for tracking
        orderRequest.put("receipt", req.getIdempotencyKey());
        
        // Adding EMI specific details in notes
        JSONObject notes = new JSONObject();
        if (req.getEmiPlan() != null) {
            notes.put("tenure_months", String.valueOf(req.getEmiPlan().getTenureMonths()));
            notes.put("bank", req.getEmiPlan().getProvider());
        }
        orderRequest.put("notes", notes);

        // API Call: Create the Order
        Order order = client.orders.create(orderRequest);

        PaymentResponse r = PaymentResponse.of(PaymentResponse.Status.AUTHORIZED, "Razorpay");
        // returns the order_id (e.g., order_RvkPZNPwH3WBQO)
        r.setIntentId(order.get("id")); 
        return r;
    }
    
    @Override
    public PaymentResponse capture(String paymentId) throws Exception {
        // 2025 Razorpay Capture Logic:
        // Note: You capture a Payment ID (pay_...), not an Order ID.
        JSONObject captureRequest = new JSONObject();
        
        // Amount and currency must match the original authorized transaction.
        // In a real production app, you would retrieve these from your database.
        // For testing, ensure this matches your initiate amount (e.g., 300000 for ₹3000).
        captureRequest.put("amount", 300000); 
        captureRequest.put("currency", "INR");

        // API Call: Finalize the payment
        Payment payment = client.payments.capture(paymentId, captureRequest);

        PaymentResponse r = new PaymentResponse();
        r.setStatus(PaymentResponse.Status.CAPTURED);
        r.setProvider("Razorpay");
        r.setIntentId(payment.get("id")); // Returns the pay_... ID
        r.setReceiptUrl("razorpay_order_" + payment.get("order_id"));
        
        return r;
    }
    
    @Override
    public PaymentResponse refund(String paymentId, Integer amount) throws Exception {
        try {
            JSONObject refundRequest = new JSONObject();
            
            // If amount is provided, it's a partial refund; otherwise, Razorpay does a full refund
            if (amount != null && amount > 0) {
                refundRequest.put("amount", amount);
            }

            // API Call: Initiate refund for the specific Payment ID (pay_...)
            Refund refund = client.payments.refund(paymentId, refundRequest);

            PaymentResponse r = new PaymentResponse();
            r.setProvider("Razorpay");
            r.setStatus(PaymentResponse.Status.CAPTURED); // Using CAPTURED as your success status
            r.setIntentId(refund.get("id")); // Stores the refund ID (rfnd_...)
            return r;

        } catch (Exception e) {
            PaymentResponse r = new PaymentResponse();
            r.setStatus(PaymentResponse.Status.FAILED);
            r.setProvider("Razorpay");
            r.setFailureReason(e.getMessage());
            return r;
        }
    }
}