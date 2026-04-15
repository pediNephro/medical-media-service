package com.esprit.microservice.medicalmediaservice.controllers;

import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleResponseDTO;
import com.esprit.microservice.medicalmediaservice.services.IImageMedicaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/images-medicales")
@RequiredArgsConstructor
public class ImageMedicaleController {

    private final IImageMedicaleService imageMedicaleService;

    @PostMapping
    public ResponseEntity<ImageMedicaleResponseDTO> create(
            @Valid @RequestBody ImageMedicaleRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageMedicaleService.create(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<ImageMedicaleResponseDTO>> getAll() {
        return ResponseEntity.ok(imageMedicaleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageMedicaleResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(imageMedicaleService.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ImageMedicaleResponseDTO>> getByPatient(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(imageMedicaleService.getByPatientId(patientId));
    }

    @GetMapping("/patient/{patientId}/type/{type}")
    public ResponseEntity<List<ImageMedicaleResponseDTO>> getByPatientAndType(
            @PathVariable Long patientId,
            @PathVariable String type) {
        return ResponseEntity.ok(
                imageMedicaleService.getByPatientIdAndType(patientId, type));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImageMedicaleResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ImageMedicaleRequestDTO requestDTO) {
        return ResponseEntity.ok(imageMedicaleService.update(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageMedicaleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // 🔥 COMPARAISON
    @GetMapping("/patient/{patientId}/comparaison")
    public ResponseEntity<Map<String, Object>> comparaisonTemporelle(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(
                imageMedicaleService.comparaisonTemporelle(patientId));
    }

    // 🔥 IA
    @GetMapping("/patient/{patientId}/analyse-ia")
    public ResponseEntity<Map<String, Object>> analyseIAComparaison(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(
                imageMedicaleService.analyseIAComparaison(patientId));
    }
}