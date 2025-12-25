package com.tcs.payments.provider;

import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class CodProvider implements PaymentProvider {
	@Override
	public PaymentResponse authorize(PaymentRequest req) {
		PaymentResponse r = PaymentResponse.of(PaymentResponse.Status.AUTHORIZED,"COD");
		r.setIntentId("cod_"+System.currentTimeMillis());
		return r;	
	}
	
	@Override
	public PaymentResponse capture(String intentId) {
		PaymentResponse r = PaymentResponse.of(PaymentResponse.Status.CAPTURED,"COD");
		r.setIntentId(intentId);;
		r.setReceiptUrl("cash_receipt_local");;
		return r;
	}
	
	  @Override
	    public PaymentResponse refund(String intentId, Integer amount) {
	        // COD refunds are handled manually (Cash back or Store Credit)
	        // We return a SUCCESS/CAPTURED status to indicate the request was acknowledged
	        PaymentResponse r = new PaymentResponse();
	        r.setProvider("COD");
	        r.setIntentId(intentId);
	        
	        // Use the status that your controller recognizes as a success (CAPTURED or SUCCESS)
	        r.setStatus(PaymentResponse.Status.CAPTURED); 
	        
	        r.setFailureReason("Manual refund required for COD. Amount: " + (amount != null ? amount : "Full"));
	        return r;
	    }
	
}


