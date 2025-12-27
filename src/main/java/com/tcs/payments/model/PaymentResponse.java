package com.tcs.payments.model;

public class PaymentResponse {

	public enum Status { 
		REQUIRES_ACTION, AUTHORIZED, CAPTURED, FAILED
	}
	private Status status;
	private String provider;
	private String intentId;
	private String clientSecret;
	private String redirectUrl;
	private String failureReason;
	private String receiptUrl;
	
	public static PaymentResponse of(Status status, String provider) {
		PaymentResponse r = new PaymentResponse();
		r.status = status;
		r.provider = provider;
		return r;		
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getIntentId() {
		return intentId;
	}

	public void setIntentId(String intentId) {
		this.intentId = intentId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public String getReceiptUrl() {
		return receiptUrl;
	}

	public void setReceiptUrl(String receiptUrl) {
		this.receiptUrl = receiptUrl;
	}
	
	
	
}