package com.tcs.payments.policy;

import com.tcs.payments.model.RegionCode;

public class PolicyResult {
    public enum Status { PASS, FAIL }
    private final Status status;
    private final String failureReason;
    private final RegionCode region;

    private PolicyResult(Status status, String reason, RegionCode region) {
        this.status = status; this.failureReason = reason; this.region = region;
    }
    public static PolicyResult pass(RegionCode region) { return new PolicyResult(Status.PASS, null, region); }
    public static PolicyResult fail(String reason) { return new PolicyResult(Status.FAIL, reason, null); }
	public Status getStatus() {
		return status;
	}
	public String getFailureReason() {
		return failureReason;
	}
	public RegionCode getRegion() {
		return region;
	}


    
}