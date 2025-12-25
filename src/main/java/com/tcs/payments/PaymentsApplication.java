package com.tcs.payments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tcs.payments.service.PaymentsProperties;

@SpringBootApplication
@EnableConfigurationProperties(PaymentsProperties.class)
public class PaymentsApplication {

	public static void main(String[] args) {
	SpringApplication.run(PaymentsApplication.class, args);

	}

}
