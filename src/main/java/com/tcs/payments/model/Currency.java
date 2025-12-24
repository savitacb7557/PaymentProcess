package com.tcs.payments.model;


import java.util.Locale;

public enum Currency {
  USD, EUR, INR, GBP, AUD, SGD, AED, JPY, CAD;

  /** Lowercase ISO code for gateways (e.g., 'usd', 'inr'). */
  public String code() {
    return name().toLowerCase(Locale.ROOT);
  }
}

