package com.tcs.payments.provider;

import java.util.HashMap;
import java.util.Map;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
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
		PaymentIntentCreateParams.Builder b = PaymentIntentCreateParams.builder()
				.setAmount(req.getAmount().getValue())
				.setCurrency(req.getAmount().getCurrency().name().toLowerCase())
				.setPaymentMethod(req.getCardToken())
				.setConfirm(true) 
				.putAllMetadata(req.getMetadata() == null ? java.util.Map.of() :
					req.getMetadata());
	
	PaymentIntentCreateParams.PaymentMethodOptions.Builder pmo = PaymentIntentCreateParams.PaymentMethodOptions.builder();
	PaymentIntentCreateParams.PaymentMethodOptions.Card.Builder card = PaymentIntentCreateParams.PaymentMethodOptions.Card.builder();
	card.setRequestThreeDSecure(request3ds ? PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.ANY:
		 PaymentIntentCreateParams.PaymentMethodOptions.Card.RequestThreeDSecure.AUTOMATIC);
	pmo.setCard(card.build());
	b.setPaymentMethodOptions(pmo.build());
	PaymentIntent intent = PaymentIntent.create(b.build(),new com.stripe.net.RequestOptions.RequestOptionsBuilder().setIdempotencyKey(req.getIdempotencyKey()).build());
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
		default: r.setStatus(PaymentResponse.Status.AUTHORIZED);
		r.setIntentId(intent.getId());
		r.setClientSecret(intent.getClientSecret());	
	}
	return r;
}
	

@Override
public PaymentResponse capture(String intentId) throws Exception {
  // Expand latest_charge during capture so you have the charge populated in the response
  Map<String, Object> captureParams = new HashMap<>();
  Map<String, Object> expand = new HashMap<>();
  captureParams.put("expand", java.util.List.of("latest_charge")); // or use a RequestOptions with expand

  PaymentIntent intent = PaymentIntent.retrieve(intentId);
  intent = intent.capture(RequestOptions.getDefault());

  Charge latestCharge = (Charge) intent.getLatestChargeObject();
  String receiptUrl = latestCharge != null ? latestCharge.getReceiptUrl() : null;

  PaymentResponse r = new PaymentResponse();
  r.setStatus(PaymentResponse.Status.CAPTURED);
  r.setProvider("Stripe");
  r.setIntentId(intent.getId());
  r.setReceiptUrl(receiptUrl);
  return r;
}

}
				
		
