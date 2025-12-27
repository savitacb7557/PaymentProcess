package com.tcs.payments.policy;


import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.tcs.payments.model.RegionCode;


@Component
public class RegionResolver {

    private static final Logger log = Logger.getLogger(RegionResolver.class.getName());

    public RegionCode resolveByCurrency(String currency) {
        if (currency == null) {
            log.fine("resolveByCurrency: input currency is null");
            return null;
        }
        final String cur = currency.trim().toUpperCase(Locale.ROOT);
        log.info("[RegionResolver] resolving currency=" + cur);

        // ---- Fast path: direct mapping (deterministic) ----
        switch (cur) {
            case "INR": return RegionCode.IN;
            case "USD": return RegionCode.US;
            case "EUR": return RegionCode.EU;
            case "GBP": return RegionCode.UK;
            case "AUD": return RegionCode.AU;
            case "SGD": return RegionCode.SG;
            case "AED": return RegionCode.AE;
            case "JPY": return RegionCode.JP;
            case "CAD": return RegionCode.CA;
            default: /* fall through to POLICIES scan */ 
        }

        // ---- Fallback: scan POLICIES (normalized) ----
        if (RegionPolicy.POLICIES == null || RegionPolicy.POLICIES.isEmpty()) {
            log.warning("[RegionResolver] POLICIES empty; cannot resolve for currency=" + cur);
            return null;
        }

        for (Map.Entry<RegionCode, RegionPolicy.Policy> e : RegionPolicy.POLICIES.entrySet()) {
            RegionPolicy.Policy pol = e.getValue();
            if (pol == null || pol.currency == null) {
                continue;
            }
            final String policyCurrency = pol.currency.trim().toUpperCase(Locale.ROOT);
            if (cur.equals(policyCurrency)) {
                return e.getKey();
            }
        }

        log.info("[RegionResolver] no region matched for currency=" + cur);
        return null; // -> region_not_supported
    }
}

