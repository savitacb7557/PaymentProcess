package com.tcs.payments.security;

import com.tcs.payments.risk.FraudEngine;

public class ThreeDSDecider {

	public enum Pref {
		PREFER_NOT, ALWAYS, RISK_BASED }
	public static class Decision {
		public boolean request3ds;
		public boolean challengePreferred;
		public String reason;
	}
	public Decision decide(Pref pref,FraudEngine.Action action) {
		Decision d =new Decision();
		if(action == FraudEngine.Action.BLOCK) {
			d.request3ds = false;
			d.reason = "blocked";
			return d;
		}
		if(pref == Pref.ALWAYS) {
			d.request3ds = true;
			return d;
		}
		if(action == FraudEngine.Action.REQUEST_3DS) {
			d.request3ds = true;
			d.challengePreferred = true;
			d.reason = "risk_based";
			return d;
		}
		d.request3ds = false;
			d.reason = "prefer_not";
			return d;
		}
	}

