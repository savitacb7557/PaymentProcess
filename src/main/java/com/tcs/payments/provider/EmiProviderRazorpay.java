package com.tcs.payments.provider;

import org.json.JSONObject;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class EmiProviderRazorpay implements PaymentProvider {
	private final RazorpayClient client;
	public EmiProviderRazorpay(String keyId, String keySecret) throws Exception {
		this.client = new RazorpayClient(keyId,keySecret);
}
	
	@Override
	public PaymentResponse authorize(PaymentRequest req) throws Exception {
		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount",req.getAmount().getValue());
		orderRequest.put("currency",req.getAmount().getCurrency().name());
		orderRequest.put("receipt", req.getIdempotencyKey());
		JSONObject notes = new JSONObject();
		notes.put("tenure",String.valueOf(req.getEmiPlan() != null ? 
				req.getEmiPlan().getTenureMonths() : "")); 
		notes.put("provider",req.getEmiPlan() != null ? req.getEmiPlan().getProvider() : "");
		orderRequest.put("notes",notes);
		Order order = client.orders.create(orderRequest);
		PaymentResponse r = PaymentResponse.of(PaymentResponse.Status.AUTHORIZED, "Razorpay");
		r.setIntentId(order.get("id"));
		return r;
	}
	
	@Override
	public PaymentResponse capture(String intentId) {
		 return PaymentResponse.of(PaymentResponse.Status.AUTHORIZED,"Razorpay");
	}

}
