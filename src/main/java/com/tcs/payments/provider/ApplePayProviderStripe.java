package com.tcs.payments.provider;


import com.stripe.Stripe;
import com.tcs.payments.model.Currency;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;
import com.tcs.payments.provider.PaymentProvider;

public class ApplePayProviderStripe implements PaymentProvider {
	public ApplePayProviderStripe(String secrectKey) {
		 Stripe.apiKey = secrectKey;
	}
	

@Override
public PaymentResponse authorize(PaymentRequest req) throws Exception {
  PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
      .setAmount(req.getAmount().getValue())
      .setCurrency(req.getAmount().getCurrency().code()) // 'usd', 'inr', etc.
      .setPaymentMethod(req.getWalletToken())            // Apple Pay token mapped to payment_method
      .setConfirm(true)
      .putAllMetadata(req.getMetadata() == null ? java.util.Map.of() : req.getMetadata())
      .build();

  RequestOptions requestOptions = new RequestOptions.RequestOptionsBuilder()
      .setIdempotencyKey(req.getIdempotencyKey())
      .build();

  PaymentIntent intent = PaymentIntent.create(params, requestOptions);

  PaymentResponse r = new PaymentResponse();
  r.setProvider("Stripe");

  switch (intent.getStatus()) {
    case "requires_action":
      r.setStatus(PaymentResponse.Status.REQUIRES_ACTION);
      r.setIntentId(intent.getId());
      r.setClientSecret(intent.getClientSecret());
      break;
    case "requires_payment_method":
      r.setStatus(PaymentResponse.Status.FAILED);
      r.setFailureReason("requires_payment_method");
      break;
    default:
      r.setStatus(PaymentResponse.Status.AUTHORIZED);
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
