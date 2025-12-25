package com.tcs.payments.api;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcs.payments.model.PaymentRequest;
import com.tcs.payments.model.PaymentResponse;
import com.tcs.payments.service.PaymentOrchestrator;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentsController {
	
	private static final Logger log = LoggerFactory.getLogger(PaymentsController.class);
	
private final PaymentOrchestrator orchestrator;
public PaymentsController(PaymentOrchestrator orchestrator) {
	this.orchestrator = orchestrator;
}

@PostMapping("/initiate")
public ResponseEntity<?> initiate(@RequestBody PaymentRequest req){
	try {
		PaymentResponse result = orchestrator.initiate(req);
		return ResponseEntity.status(result.getStatus() == PaymentResponse.Status.FAILED ? 400 : 200) .body(result);
	}catch(Exception e) {
		log.error("initiate_error",e);
		return ResponseEntity.status(500).body(java.util.Map.of("error","server_error","message",e.getMessage()));
		
	}
}

@PostMapping("/capture")
public ResponseEntity<?> capture(@RequestBody java.util.Map<String,String> body){
	try {
		String provider = body.get("provider");
		String intentId = body.get("intentId");
		if(provider == null || intentId == null)
			return ResponseEntity.badRequest().body(java.util.Map.of("error","missing_params"));
		
		PaymentResponse result = orchestrator.capture(provider, intentId);
		return ResponseEntity.status(result.getStatus() == PaymentResponse.Status.FAILED ? 400 : 200).body(result);
	} catch(Exception e) {
		log.error("capture_error",e);
		return ResponseEntity.status(500).body(java.util.Map.of("error","server_error","message",e.getMessage()));
	}
}


//below method added for refund option
@PostMapping("/refund")
public ResponseEntity<?> refund(@RequestBody java.util.Map<String, Object> body) {
    try {
        String provider = (String) body.get("provider");
        String paymentId = (String) body.get("paymentId");
        
        // Amount is optional for full refund, mandatory for partial refund
        // Razorpay expects amount in paise (e.g., 50000 for ₹500)
        Integer amount = (Integer) body.get("amount"); 

        if (provider == null || paymentId == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "missing_params"));
        }

        // Logic: Orchestrator calls the Razorpay API to process the refund
        PaymentResponse result = orchestrator.refund(provider, paymentId, amount);
        
        return ResponseEntity.status(result.getStatus() == PaymentResponse.Status.FAILED ? 400 : 200)
                             .body(result);
    } catch (Exception e) {
        log.error("refund_error", e);
        return ResponseEntity.status(500).body(java.util.Map.of("error", "server_error", "message", e.getMessage()));
    }
}
}
