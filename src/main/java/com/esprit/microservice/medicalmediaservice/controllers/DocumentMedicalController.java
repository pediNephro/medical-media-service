package com.esprit.microservice.medicalmediaservice.controllers;

import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalResponseDTO;
import com.esprit.microservice.medicalmediaservice.services.IDocumentMedicalService;
import com.esprit.microservice.medicalmediaservice.services.PdfExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents-medicaux")
@RequiredArgsConstructor
public class DocumentMedicalController {

    private final IDocumentMedicalService documentMedicalService;
    private final PdfExportService pdfExportService;

    // =========================================================
    // CRUD DE BASE
    // =========================================================

    @PostMapping
    public ResponseEntity<DocumentMedicalResponseDTO> create(
            @Valid @RequestBody DocumentMedicalRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentMedicalService.create(requestDTO));
    }

    @GetMapping
    public ResponseEntity<List<DocumentMedicalResponseDTO>> getAll() {
        return ResponseEntity.ok(documentMedicalService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentMedicalResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentMedicalService.getById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DocumentMedicalResponseDTO>> getByPatient(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(documentMedicalService.getByPatientId(patientId));
    }

    @GetMapping("/patient/{patientId}/type/{type}")
    public ResponseEntity<List<DocumentMedicalResponseDTO>> getByPatientAndType(
            @PathVariable Long patientId, @PathVariable String type) {
        return ResponseEntity.ok(documentMedicalService.getByPatientIdAndType(patientId, type));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentMedicalResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody DocumentMedicalRequestDTO requestDTO) {
        return ResponseEntity.ok(documentMedicalService.update(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentMedicalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // OCR
    // =========================================================

    @PutMapping("/{id}/ocr")
    public ResponseEntity<DocumentMedicalResponseDTO> extractOcr(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                documentMedicalService.extractOcr(id, body.get("contenuOcr")));
    }

    // =========================================================
    // COMPLÉTUDE DOSSIER
    // =========================================================

    @GetMapping("/patient/{patientId}/completude")
    public ResponseEntity<Map<String, Object>> verifierCompletude(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(documentMedicalService.verifierCompletude(patientId));
    }

    // =========================================================
    // RECHERCHE ET FILTRE
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<DocumentMedicalResponseDTO>> search(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(documentMedicalService.searchByOcr(q));
    }

    @GetMapping("/filter")
    public ResponseEntity<?> filter(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        LocalDate localDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : null;
        return ResponseEntity.ok(
                documentMedicalService.filter(patientId, type, localDate, page, size));
    }

    // =========================================================
    // ✅ NOUVEAU — SCORE DE CONFIANCE OCR
    // =========================================================

    @PostMapping("/classifier")
    public ResponseEntity<Map<String, Object>> classifier(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                documentMedicalService.classifyWithConfidence(body.get("texte")));
    }

    // =========================================================
    // ✅ NOUVEAU — DASHBOARD STATISTIQUES
    // =========================================================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(documentMedicalService.getDashboardStats());
    }

    // =========================================================
    // ✅ NOUVEAU — EXPORT PDF DOSSIER PATIENT
    // =========================================================

    @GetMapping("/patient/{patientId}/export-pdf")
    public ResponseEntity<byte[]> exporterPdf(@PathVariable Long patientId) {
        try {
            byte[] pdf = pdfExportService.exporterDossierPatient(patientId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=dossier-patient-" + patientId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/patient/{id}/risk")
    public ResponseEntity<?> getRisk(@PathVariable Long id) {
        List<DocumentMedicalResponseDTO> docs = documentMedicalService.getByPatientId(id);

        List<Map<String, Object>> risks = docs.stream()
                .filter(d -> d.getContenuOcr() != null)
                .map(d -> documentMedicalService.analyserRisqueRenal(d.getContenuOcr()))
                .toList();

        return ResponseEntity.ok(risks);
    }
    @GetMapping("/patient/{id}/timeline")
    public ResponseEntity<?> getTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(documentMedicalService.getTimeline(id));
    }

}