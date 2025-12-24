package com.tcs.payments.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {
	
	@NotBlank private String idempotencyKey;
	@NotNull private RegionCode region;
	@NotNull private PaymentMethod method;
	@NotNull private Amount amount;
	@NotNull private Customer customer;
	
	private String cardToken;
	private String walletToken;
	
	private boolean saveCard;
	private EmiPlan emiPlan;
	private String codNote;
	private String couponCode;
	private java.util.Map<String,String> metadata;
	public String getIdempotencyKey() {
		return idempotencyKey;
	}
	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}
	public RegionCode getRegion() {
		return region;
	}
	public void setRegion(RegionCode region) {
		this.region = region;
	}
	public PaymentMethod getMethod() {
		return method;
	}
	public void setMethod(PaymentMethod method) {
		this.method = method;
	}
	public Amount getAmount() {
		return amount;
	}
	public void setAmount(Amount amount) {
		this.amount = amount;
	}
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	public String getCardToken() {
		return cardToken;
	}
	public void setCardToken(String cardToken) {
		this.cardToken = cardToken;
	}
	public String getWalletToken() {
		return walletToken;
	}
	public void setWalletToken(String walletToken) {
		this.walletToken = walletToken;
	}
	public boolean isSaveCard() {
		return saveCard;
	}
	public void setSaveCard(boolean saveCard) {
		this.saveCard = saveCard;
	}
	public EmiPlan getEmiPlan() {
		return emiPlan;
	}
	public void setEmiPlan(EmiPlan emiPlan) {
		this.emiPlan = emiPlan;
	}
	public String getCodNote() {
		return codNote;
	}
	public void setCodNote(String codNote) {
		this.codNote = codNote;
	}
	public String getCouponCode() {
		return couponCode;
	}
	public void setCouponCode(String couponCode) {
		this.couponCode = couponCode;
	}
	public java.util.Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(java.util.Map<String, String> metadata) {
		this.metadata = metadata;
	}
	

}
