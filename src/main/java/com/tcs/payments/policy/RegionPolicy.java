package com.tcs.payments.policy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.tcs.payments.model.PaymentMethod;
import com.tcs.payments.model.RegionCode;

public class RegionPolicy {

	public static class Policy {
		public String currency;
		public List<PaymentMethod> enabledMethods;
		public Coupons coupons;
		public Emi emi;
	}
	public static class Coupons {
		public boolean enabled;
		public List<String> allowedPrefixes;
	}
	public static class Emi{
		public boolean enabled;
		public List<Integer> allowedTenures;
	}
	
	public static final Map<RegionCode, Policy> POLICIES = new EnumMap<>(RegionCode.class);
	static {
		Policy IN = new Policy();
		IN.currency = "INR";
		IN.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.GOOGLE_PAY, PaymentMethod.COD, PaymentMethod.EMI, PaymentMethod.APPLE_PAY);
		IN.coupons = new Coupons();
		IN.coupons.enabled = true;
		IN.coupons.allowedPrefixes = List.of("IN","Global");
		IN.emi = new Emi();
		IN.emi.enabled = true;
		IN.emi.allowedTenures = List.of(3,6,9,12);
		POLICIES.put(RegionCode.IN,IN);
		
		Policy US = new Policy();
		US.currency = "USD";
		US.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		US.coupons = new Coupons();
		US.coupons.enabled = true;
		US.coupons.allowedPrefixes = List.of("US","Global");
		US.emi =new Emi();
		US.emi.enabled =true;
		US.emi.allowedTenures = List.of(6,12,24);
		POLICIES.put(RegionCode.US, US);
		
		Policy EU = new Policy();
		EU.currency = "EUR";
		EU.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		EU.coupons = new Coupons();
		EU.coupons.enabled = true;
		EU.coupons.allowedPrefixes = List.of("EU","Global");
		EU.emi =new Emi();
		EU.emi.enabled = true;
		EU.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.EU, EU);
		
		Policy UK = new Policy();
		UK.currency = "GBP";
		UK.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		UK.coupons = new Coupons();
		UK.coupons.enabled = true;
		UK.coupons.allowedPrefixes = List.of("UK","Global");
		UK.emi =new Emi();
		UK.emi.enabled = true;
		UK.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.UK, UK);
		
		Policy AU = new Policy();
		AU.currency = "AUD";
		AU.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		AU.coupons = new Coupons();
		AU.coupons.enabled = true;
		AU.coupons.allowedPrefixes = List.of("AU","Global");
		AU.emi =new Emi();
		AU.emi.enabled = true;
		AU.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.AU, AU);
		
		Policy SG = new Policy();
		SG.currency = "SGD";
		SG.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		SG.coupons = new Coupons();
		SG.coupons.enabled = true;
		SG.coupons.allowedPrefixes = List.of("SG","Global");
		SG.emi =new Emi();
		SG.emi.enabled = true;
		SG.emi.allowedTenures=List.of(3,6,12);
		POLICIES.put(RegionCode.SG, SG);
		
		Policy AE = new Policy();
		AE.currency = "AED";
		AE.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		AE.coupons = new Coupons();
		AE.coupons.enabled = true;
		AE.coupons.allowedPrefixes = List.of("SG","Global");
		AE.emi =new Emi();
		AE.emi.enabled = true;
		AE.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.AE, AE);
		
		Policy JP = new Policy();
		JP.currency = "JPY";
		JP.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		JP.coupons = new Coupons();
		JP.coupons.enabled = true;
		JP.coupons.allowedPrefixes = List.of("JP","Global");
		JP.emi =new Emi();
		JP.emi.enabled = true;
		JP.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.JP, JP);
		
		Policy CA = new Policy();
		CA.currency = "CAD";
		CA.enabledMethods = List.of(PaymentMethod.CARD, PaymentMethod.APPLE_PAY,PaymentMethod.GOOGLE_PAY,PaymentMethod.EMI,PaymentMethod.COD);
		CA.coupons = new Coupons();
		CA.coupons.enabled = true;
		CA.coupons.allowedPrefixes = List.of("JP","Global");
		CA.emi =new Emi();
		CA.emi.enabled = true;
		CA.emi.allowedTenures=List.of(6,12);
		POLICIES.put(RegionCode.CA, CA);
		
		
	}
}
