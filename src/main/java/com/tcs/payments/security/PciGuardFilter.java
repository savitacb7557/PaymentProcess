package com.tcs.payments.security;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class PciGuardFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only inspect relevant requests
        if ("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api")) {

            // Wrap to allow multiple reads
            MultiReadHttpServletRequest wrapped = new MultiReadHttpServletRequest(request);

            String body = wrapped.getCachedBody(wrapped.getCharacterEncoding());
            request.setAttribute("rawBody", body); // optional

            if (containsPciData(body)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"pci_violation\",\"message\":\"Do not send PAN/CVV; use PSP tokenization.\"}");
                return; // BLOCK the request here
            }

            // Pass the wrapped request so Spring can bind @RequestBody safely
            filterChain.doFilter(wrapped, response);
            return;
        }

        // Non-POST or non-/api paths
        filterChain.doFilter(request, response);
    }

    private boolean containsPciData(String body) {
        boolean hasPan = body.matches(".*\\b\\d{12,19}\\b.*");
        boolean hasCvv = body.matches(".*\\b(cvv|cvc)\\b.*");
        boolean hasExpiry = body.matches(".*\\b(expiry|expMonth|expYear)\\b.*");
        boolean hasCardNumberKey = body.matches(".*\\b(cardNumber)\\b.*");
        return hasPan || hasCvv || hasExpiry || hasCardNumberKey;
    }
}
