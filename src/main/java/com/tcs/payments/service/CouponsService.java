package com.tcs.payments.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tcs.payments.model.Amount;
import com.tcs.payments.model.RegionCode;

public class CouponsService {

	private static class Coupon {
		Integer percentOff;
		Long amountOff;
		Set<RegionCode> regions;
		LocalDate expiresAt;
	}
	private static final Map<String, Coupon> DB = new HashMap<>();

static {
    Coupon in10 = new Coupon();
    in10.percentOff = 10;
    // Single element (enum-friendly)
    in10.regions = EnumSet.of(RegionCode.IN);
    in10.expiresAt = LocalDate.of(2026, 1, 31);
    DB.put("IN-FESTIVE-10", in10);

    Coupon g5 = new Coupon();
    g5.percentOff = 5;

    // Multiple regions:
    // Option A: EnumSet.copyOf(Arrays.asList(...)) — Java 8 compatible
    in10.expiresAt = LocalDate.of(2026, 1, 31);
    g5.regions = EnumSet.copyOf(Arrays.asList(
        RegionCode.US, RegionCode.EU, RegionCode.UK, RegionCode.IN,
        RegionCode.AU, RegionCode.SG, RegionCode.AE, RegionCode.JP, RegionCode.CA
    ));

    // If you need it to be immutable (optional):
    // g5.regions = Collections.unmodifiableSet(
    //     EnumSet.copyOf(Arrays.asList(
    //         RegionCode.US, RegionCode.EU, RegionCode.UK, RegionCode.IN,
    //         RegionCode.AU, RegionCode.SG, RegionCode.AE, RegionCode.JP, RegionCode.CA
    //     ))
    // );

    g5.expiresAt = LocalDate.of(2026, 1, 31);
    DB.put("GLOBAL-5", g5); // or whatever key you use for the 5% global coupon
}

	public static class Result{
		public Amount finalAmount;
		public boolean applied;
		public String reason;
	
	 public Amount finalAmount() { return finalAmount; }
	}
	 
	public Result apply(RegionCode region,Amount amount,String code) {
		Result r = new Result();
		r.finalAmount = amount;
		r.applied = false;
		if(code == null || code.isBlank()) return r;
		Coupon c = DB.get(code);
		if(c == null) {
			r.reason = "invalie_coupon";
			return r;
		}
		if(!c.regions.contains(region)) {
			r.reason = "coupon_not_applicable_for_region";
			return r;
		}
		if(c.expiresAt != null && c.expiresAt.isBefore(LocalDate.now())) {
			r.reason = "coupon_expired";
			return r;
		}
		long value = amount.getValue();
		if(c.percentOff != null)  value = Math.max(0, value - Math.round(value * (c.percentOff/100.0)));
		else if(c.amountOff != null) value =Math.max(0, value - c.amountOff);
		Amount finalA = new Amount(value,amount.getCurrency());
		r.finalAmount = finalA;
		r.applied = true;
		return r;
	}
}
