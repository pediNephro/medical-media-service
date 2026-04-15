package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IDocumentMedicalService {

    DocumentMedicalResponseDTO create(DocumentMedicalRequestDTO requestDTO);
    DocumentMedicalResponseDTO getById(Long id);
    List<DocumentMedicalResponseDTO> getAll();
    List<DocumentMedicalResponseDTO> getByPatientId(Long patientId);
    List<DocumentMedicalResponseDTO> getByPatientIdAndType(Long patientId, String type);
    DocumentMedicalResponseDTO update(Long id, DocumentMedicalRequestDTO requestDTO);
    void delete(Long id);

    // Fonction OCR
    DocumentMedicalResponseDTO extractOcr(Long id, String contenuOcr);

    // Fonction complétude dossier
    Map<String, Object> verifierCompletude(Long patientId);

    // Recherche OCR
    List<DocumentMedicalResponseDTO> searchByOcr(String keyword);

    // Filtre paginé
    Page<DocumentMedicalResponseDTO> filter(Long patientId, String type, LocalDate date, int page, int size);

    // ✅ NOUVEAU — score de confiance OCR
    Map<String, Object> classifyWithConfidence(String texte);

    // ✅ NOUVEAU — dashboard statistiques
    Map<String, Object> getDashboardStats();
    // 🔥 NOUVEAU — analyse risque rénal
    Map<String, Object> analyserRisqueRenal(String texte);

    // 🔥 NOUVEAU — timeline patient
    List<Map<String, Object>> getTimeline(Long patientId);
}