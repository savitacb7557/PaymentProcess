package com.tcs.payments.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcs.payments.model.RegionCode;
import com.tcs.payments.policy.RegionPolicy;
import com.tcs.payments.policy.RegionResolver;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/policies")
    public Map<String, Object> dumpPolicies() {
        Map<String, Object> out = new HashMap<>();
        out.put("size", RegionPolicy.POLICIES.size());
        out.put("keys", RegionPolicy.POLICIES.keySet());

        Map<RegionCode, String> currencies = new EnumMap<>(RegionCode.class);
        for (var e : RegionPolicy.POLICIES.entrySet()) {
            currencies.put(e.getKey(), e.getValue().currency);
        }
        out.put("currencies", currencies);
        out.put("regionPolicyClassLoader", String.valueOf(RegionPolicy.class.getClassLoader()));
        out.put("regionCodeClassLoader", String.valueOf(RegionCode.class.getClassLoader()));
        return out;
    }

    @GetMapping("/resolve")
    public Map<String, Object> resolve(@RequestParam String currency, RegionResolver resolver) {
        Map<String, Object> out = new HashMap<>();
        out.put("input", currency);
        out.put("resolved", resolver.resolveByCurrency(currency));
        return out;
    }
}