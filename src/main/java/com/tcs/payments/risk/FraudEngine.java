package com.tcs.payments.risk;

import java.util.ArrayList;
import java.util.List;

import com.tcs.payments.model.PaymentRequest;

public class FraudEngine {
	private final int SCORE_3DS = 50;
	private final int SCORE_BLOCK =80;
	public static class Verdict {
		public int score;
		public Action action;
		public List<String> reasons;
	}
	public enum Action {
		ALLOW,REVIEW,BLOCK,REQUEST_3DS
	}
	
	public Verdict evaluate(PaymentRequest req) {
		int score = 0;
		List<String> reasons = new ArrayList<>();
		if(req.getAmount().getValue() >= 100000) {
			score +=30;
			reasons.add("high_amount");
		}
		if(req.getRegion() == com.tcs.payments.model.RegionCode.IN &&
				req.getMethod() == com.tcs.payments.model.PaymentMethod.APPLE_PAY) {
			score +=25;
			reasons.add("region_method_mismatch");
		}
		if((req.getCustomer().getEmail() == null || 
			req.getCustomer().getEmail().isBlank()) &&
		(req.getCustomer().getPhone() == null ||
		req.getCustomer().getPhone().isBlank())){
			score +=10;
			reasons.add("low_customer_signal");
		}
		if(req.getCouponCode() != null &&
				req.getCouponCode().startsWith("GLOBAL")) {
			score +=5;
			reasons.add("generic_coupon");
		}
		Verdict v = new Verdict();
		v.score = score;
		v.reasons = reasons;
		if(score >= SCORE_BLOCK) {
			v.action = Action.BLOCK;
			return v;
		}
		if(score >= SCORE_3DS) {
			v.action = Action.REQUEST_3DS;
			return v;
		}
		v.action = (score >= 40) ? Action.REVIEW : Action.ALLOW;
		return v;
	}

}
