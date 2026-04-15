package com.esprit.microservice.medicalmediaservice.controllers;

import com.esprit.microservice.medicalmediaservice.services.PatientIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class PatientIntelligenceController {

    private final PatientIntelligenceService service;

    @GetMapping("/risk/{patientId}")
    public ResponseEntity<?> risk(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.calculRisk(patientId));
    }

    @GetMapping("/assistant/{patientId}")
    public ResponseEntity<?> assistant(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.assistantMedical(patientId));
    }

    @GetMapping("/qualite/{patientId}")
    public ResponseEntity<?> qualite(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.scoreQualite(patientId));
    }

    @GetMapping("/priorite/{patientId}")
    public ResponseEntity<?> priorite(@PathVariable Long patientId) {
        return ResponseEntity.ok(service.priorite(patientId));
    }
}