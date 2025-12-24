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
	
}


