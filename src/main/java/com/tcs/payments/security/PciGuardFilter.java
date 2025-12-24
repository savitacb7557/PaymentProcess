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
public class PciGuardFilter extends OncePerRequestFilter{

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
	throws ServletException,IOException {
		if("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/api")) {
			String body = request.getReader().lines().collect(Collectors.joining(""));
			if(body != null && body.matches(".*(cardNumber|cvv|cvc|expiry).*")) {

response.setStatus(400);
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");

response.getWriter().write("{\"error\":\"pci_violation\",\"message\":\"Do not send PAN/CVV; use PSP tokenization.\"}");
                  return;
			}
			request.setAttribute("rawBody", body);
		}
		filterChain.doFilter(request, response);
	}
}
