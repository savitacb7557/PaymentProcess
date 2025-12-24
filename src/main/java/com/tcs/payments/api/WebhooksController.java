package com.tcs.payments.api;

import java.io.IOException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.stripe.model.Event; 
import jakarta.servlet.http.HttpServletRequest;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import com.stripe.Stripe;

@RestController
@RequestMapping("/api/webhooks")
public class WebhooksController {
	
	private static final Logger log = LoggerFactory.getLogger(WebhooksController.class);
	@Value("${payments.stripe.secretKey}") String stripeKey;
	@Value("${payments.stripe.webhookSecret}") String stripeWebhookSecret;
	@Value("${payments.razorpay.keySecret}") String razorpayWebhookSecret;
	
	
	@PostMapping("/stripe")
	public ResponseEntity<?> stripe(HttpServletRequest request, @RequestHeader("Stripe-Signature") String sig) throws IOException {
		Stripe.apiKey = stripeKey;
		String payload = request.getReader().lines().collect(Collectors.joining(""));
		try {
			Event event = Webhook.constructEvent(payload,sig,stripeWebhookSecret);
			log.info("stripe event: {}", event.getType());
			return ResponseEntity.ok(java.util.Map.of("received", true));
		} catch (SignatureVerificationException e) {
			log.error("stripe webhook signature failed",e);
			return ResponseEntity.badRequest().body(java.util.Map.of("error","invalid_signature"));
		}
	}
	
	@PostMapping("/razorpay")
	public ResponseEntity<?> razorpay(@RequestBody String body, @RequestHeader(value="X-Razorpay-Signature", required=false) String sig){
		log.info("razorpay event:body-length={}", body.length());
		return ResponseEntity.ok(java.util.Map.of("received", true));
	}
	

}
