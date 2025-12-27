package com.tcs.payments.model;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class Amount {
  @Min(0)
  private long value; // minor units
  @NotNull
  private Currency currency; // <-- must be com.tcs.payments.model.Currency

  public Amount() {}
  public Amount(long value, Currency currency) { this.value = value; this.currency = currency; }

  public long getValue() { return value; }
  public void setValue(long value) { this.value = value; }
  public Currency getCurrency() { return currency; }           // returns your enum
  public void setCurrency(Currency currency) { this.currency = currency; }
}
