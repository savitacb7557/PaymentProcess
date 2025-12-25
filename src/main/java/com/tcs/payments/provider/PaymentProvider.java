package com.tcs.payments.provider;

import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public interface PaymentProvider {
	 PaymentResponse authorize(PaymentRequest req) throws Exception;
	 PaymentResponse capture(String intentId) throws Exception;
	 PaymentResponse refund(String paymentId, Integer amount) throws Exception;

}
