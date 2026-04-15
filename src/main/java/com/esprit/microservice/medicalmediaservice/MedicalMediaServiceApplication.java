package com.esprit.microservice.medicalmediaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MedicalMediaServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MedicalMediaServiceApplication.class, args);
	}
}