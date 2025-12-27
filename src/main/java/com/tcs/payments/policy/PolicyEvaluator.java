package com.tcs.payments.policy;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.tcs.payments.model.Amount;
import com.tcs.payments.model.Currency;
import com.tcs.payments.model.EmiPlan;
import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.RegionCode;

/**
 * Evaluates a PaymentRequest against region and provider policies.
 *
 * This version matches the user's existing code style:
 * - RegionPolicy.Policy fields: currency, enabledMethods, emi
 * - EMI checks via EmiPlan
 * - Provider switch on lowercase strings: "stripe", "razorpay", "cod"
 * - Returns PolicyResult.pass(region) on success
 */

@Component
public class PolicyEvaluator {

    private static final Logger log = Logger.getLogger(PolicyEvaluator.class.getName());

    private final RegionResolver resolver;

    // If these allow-lists exist elsewhere in your codebase, remove these and import/use the existing ones.
    private static final Set<String> STRIPE_ALLOWED = Set.of(
        // ISO 4217 codes allowed for Stripe in your system
        "USD", "EUR", "GBP", "AUD", "CAD", "SGD", "JPY", "NZD", "CHF", "HKD", "SEK", "NOK", "DKK", "MXN", "BRL", "ZAR", "AED", "SAR", "INR"
    );

    private static final Set<String> RAZORPAY_ALLOWED = Set.of(
        // Razorpay generally supports INR; add others only if supported in your system
        "INR"
    );

    private static final Set<String> COD_ALLOWED = Set.of(
        // Currencies where COD is permitted in your system
        "INR" // add more if applicable
    );

    public PolicyEvaluator(RegionResolver resolver) {
        this.resolver = resolver;
    }

    public PolicyResult evaluate(PaymentRequest req) {
        // --- Guard: request ---
        if (req == null) {
            log.warning("Null PaymentRequest");
            return PolicyResult.fail("request_missing");
        }

        // --- Guard: currency presence ---
        Amount amt = req.getAmount();
        if (amt == null || amt.getCurrency() == null) {
            return PolicyResult.fail("currency_missing");
        }

        // Currency enum -> name()
        Currency curEnum = amt.getCurrency();
        String reqCur = toIso4217(curEnum);


log.info("[PolicyEvaluator] reqCur=" + reqCur);

// Classloader identity (helps find duplicate classes/modules issues)
log.info("[Debug] RegionPolicy classLoader=" + RegionPolicy.class.getClassLoader());
log.info("[Debug] RegionCode classLoader=" + RegionCode.class.getClassLoader());

// Map identity and content
log.info("[Debug] POLICIES identHash=" + System.identityHashCode(RegionPolicy.POLICIES));
log.info("[PolicyEvaluator] policies size=" + (RegionPolicy.POLICIES != null ? RegionPolicy.POLICIES.size() : -1));
log.info("[PolicyEvaluator] policies keys=" + RegionPolicy.POLICIES.keySet());

RegionCode region = resolver.resolveByCurrency(reqCur);
log.info("[PolicyEvaluator] resolver returned region=" + region);

RegionPolicy.Policy pol = (region != null) ? RegionPolicy.POLICIES.get(region) : null;
log.info("[PolicyEvaluator] policy exists for region? " + (pol != null));
if (pol != null) {
    log.info("[PolicyEvaluator] policy.currency=" + pol.currency);
    log.info("[PolicyEvaluator] enabledMethods=" + pol.enabledMethods);
}



        // --- Currency must match policy currency (normalize case) ---
        if (pol.currency == null) {
            log.warning(String.format("Policy currency missing for region=%s", region));
            return PolicyResult.fail("policy_currency_missing");
        }
        String polCur = pol.currency.trim().toUpperCase(Locale.ROOT);
        if (!reqCur.equals(polCur)) {
            log.warning(String.format("Currency mismatch: req=%s policy=%s", reqCur, polCur));
            return PolicyResult.fail("currency_mismatch");
        }

        // --- Method supported for region? ---
        if (pol.enabledMethods == null || !pol.enabledMethods.contains(req.getMethod())) {
            log.warning("Method not supported for region: " + req.getMethod());
            return PolicyResult.fail("method_not_supported");
        }

        // --- Provider-level currency constraints ---
        String provider = req.getProvider() != null ? req.getProvider().trim().toLowerCase(Locale.ROOT) : "";
        switch (provider) {
            case "stripe":
                if (!STRIPE_ALLOWED.contains(reqCur)) {
                    log.warning("Stripe not allowed for currency: " + reqCur);
                    return PolicyResult.fail("currency_not_allowed_for_provider");
                }
                break;
            case "razorpay":
                if (!RAZORPAY_ALLOWED.contains(reqCur)) {
                    log.warning("Razorpay not allowed for currency: " + reqCur);
                    return PolicyResult.fail("currency_not_allowed_for_provider");
                }
                break;
            case "cod":
            case "":
                if (!COD_ALLOWED.contains(reqCur)) {
                    log.warning("COD not allowed for currency: " + reqCur);
                    return PolicyResult.fail("currency_not_allowed_for_provider");
                }
                break;
            default:
                log.warning("Unknown provider: " + provider);
                return PolicyResult.fail("provider_not_supported");
        }

        // --- Optional: amount basic validation (value present & positive) ---
        BigDecimal value = BigDecimal.valueOf(amt.getValue());
        if (value == null) {
            return PolicyResult.fail("amount_missing");
        }
        if (value.signum() <= 0) {
            return PolicyResult.fail("amount_non_positive");
        }

        // ----- EMI validation using EmiPlan -----
        EmiPlan plan = req.getEmiPlan(); // presence => EMI requested (when method == EMI or method == CARD+emi)
        if (plan != null) {
            // Region must enable EMI
            if (pol.emi == null || !pol.emi.enabled) {
                return PolicyResult.fail("emi_not_enabled_for_region");
            }
            // Tenure must be allowed for region (if tenure > 0)
            int tenure = plan.getTenureMonths();
            if (tenure > 0 && (pol.emi.allowedTenures == null || !pol.emi.allowedTenures.contains(tenure))) {
                return PolicyResult.fail("emi_tenure_not_allowed");
            }
            // Optional: validate EMI provider for currency (e.g., Razorpay only for INR)
            String emiProvider = plan.getProvider() != null ? plan.getProvider().trim().toLowerCase(Locale.ROOT) : "";
            if (!emiProvider.isBlank()) {
                if ("razorpay".equals(emiProvider) && !RAZORPAY_ALLOWED.contains(reqCur)) {
                    return PolicyResult.fail("emi_provider_currency_mismatch");
                }
                if ("stripe".equals(emiProvider) && !STRIPE_ALLOWED.contains(reqCur)) {
                    return PolicyResult.fail("emi_provider_currency_mismatch");
                }
            }
        }

        // --- All checks passed ---
        return PolicyResult.pass(region);
    }
    

/**
 * Maps your custom Currency enum to ISO 4217 code (uppercase).
 * If your enum already uses ISO names, this just returns the enum name.
 * Add/adjust cases if your enum uses non-ISO names like RUPEE, DOLLAR, etc.
 */
private static String toIso4217(Currency c) {
    if (c == null) return null;

    final String name = c.name().trim().toUpperCase(java.util.Locale.ROOT);

    // If your enum already uses ISO codes, this immediately works:
    switch (name) {
        case "INR": case "USD": case "EUR": case "GBP":
        case "AUD": case "SGD": case "AED": case "JPY": case "CAD":
            return name;
    }

    // If your enum uses non-ISO names, add mappings here:
    switch (name) {
        case "RUPEE":       return "INR";
        case "US_DOLLAR":   return "USD";
        case "EURO":        return "EUR";
        case "POUND":       return "GBP";
        case "YEN":         return "JPY";
        // Add any aliases you actually have
        default:
            // Fallback: return the enum name uppercased
            return name;
    }
}

}
