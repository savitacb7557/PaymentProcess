package com.tcs.payments.provider;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;

public class GooglePayProviderStripe implements PaymentProvider {
	public GooglePayProviderStripe(String secretKey) {
		Stripe.apiKey = secretKey;
	}
	
@Override
public PaymentResponse authorize(PaymentRequest req) throws Exception {
	PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
			.setAmount(req.getAmount().getValue())
			.setCurrency(req.getAmount().getCurrency().name().toLowerCase())
			.setPaymentMethod(req.getWalletToken())
			.setConfirm(true)
			.putAllMetadata(req.getMetadata() == null ? java.util.Map.of() :
				req.getMetadata()).build();
	PaymentIntent intent = PaymentIntent.create(params, new com.stripe.net.RequestOptions.RequestOptionsBuilder().setIdempotencyKey(req.getIdempotencyKey()).build());
	PaymentResponse r = new PaymentResponse();
	r.setProvider("Stripe");
	switch(intent.getStatus()) {
	case "requires_action":
		r.setStatus(PaymentResponse.Status.REQUIRES_ACTION);
		r.setIntentId(intent.getId());
		r.setClientSecret(intent.getClientSecret());
		break;
	case "requires_payment_method":
	r.setStatus(PaymentResponse.Status.FAILED);
	r.setFailureReason("requires_payment_method");
	break;
	default: r.setStatus(PaymentResponse.Status.AUTHORIZED);
	r.setIntentId(intent.getId());
	r.setClientSecret(intent.getClientSecret());
	}
	return r;
}

@Override
public PaymentResponse capture(String intentId) throws Exception {
	PaymentIntent intent = PaymentIntent.retrieve(intentId).capture();
	PaymentResponse r = new PaymentResponse();
	r.setStatus(PaymentResponse.Status.CAPTURED);
	r.setProvider("Stripe");
	r.setIntentId(intent.getId());
	return r;
}

}
