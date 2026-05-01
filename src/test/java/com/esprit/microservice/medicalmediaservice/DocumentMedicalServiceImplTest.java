package com.esprit.microservice.medicalmediaservice;

import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalResponseDTO;
import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import com.esprit.microservice.medicalmediaservice.repositories.DocumentMedicalRepository;

import com.esprit.microservice.medicalmediaservice.services.DocumentMedicalServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocumentMedicalServiceImplTest {

    private final DocumentMedicalRepository repo = mock(DocumentMedicalRepository.class);
    private final DocumentMedicalServiceImpl service = new DocumentMedicalServiceImpl(repo);

    // =============================
    // 🔥 TEST IA
    // =============================

    @Test
    void testClassificationBilan() {
        String text = "créatinine 120 urée 8 mmol";

        Map<String, Object> result = service.classifyWithConfidence(text);

        assertEquals("BILAN", result.get("type"));
        assertTrue((Integer) result.get("score") > 0);
    }

    @Test
    void testClassificationOrdonnance() {
        String text = "ordonnance medicament 500mg";

        Map<String, Object> result = service.classifyWithConfidence(text);

        assertEquals("ORDONNANCE", result.get("type"));
    }

    @Test
    void testTexteVide() {
        Map<String, Object> result = service.classifyWithConfidence("");

        assertEquals("AUTRE", result.get("type"));
    }

    // =============================
    // 🔥 TEST RISQUE RÉNAL
    // =============================

    @Test
    void testRisqueRenalCritique() {
        String text = "créatinine 150 urée 12 potassium 6";

        Map<String, Object> result = service.analyserRisqueRenal(text);

        assertEquals("CRITIQUE", result.get("risk"));
    }

    // =============================
    // 🔥 TEST CREATE
    // =============================

    @Test
    void testCreateDocument() {

        DocumentMedical saved = DocumentMedical.builder()
                .id(1L)
                .nomFichier("test.pdf")
                .type("BILAN")
                .dateUpload(LocalDate.now())
                .patientId(1L)
                .build();

        when(repo.save(any())).thenReturn(saved);

        DocumentMedicalRequestDTO dto = DocumentMedicalRequestDTO.builder()
                .nomFichier("test.pdf")
                .dateUpload(LocalDate.now())
                .contenuOcr("créatinine 120 urée 8")
                .patientId(1L)
                .build();

        DocumentMedicalResponseDTO result = service.create(dto);

        assertNotNull(result);
        assertEquals("BILAN", result.getType());
    }

    // =============================
    // 🔥 TEST EXCEPTION
    // =============================

    @Test
    void testGetByIdNotFound() {

        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.getById(1L));
    }
    @Test
    void testUpdateDocument() {

        DocumentMedical existing = DocumentMedical.builder()
                .id(1L)
                .nomFichier("old.pdf")
                .dateUpload(LocalDate.now())
                .patientId(1L)
                .build();

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenReturn(existing);

        DocumentMedicalRequestDTO dto = DocumentMedicalRequestDTO.builder()
                .nomFichier("new.pdf")
                .dateUpload(LocalDate.now())
                .patientId(1L)
                .build();

        DocumentMedicalResponseDTO result = service.update(1L, dto);

        assertEquals("new.pdf", result.getNomFichier());
    }


}