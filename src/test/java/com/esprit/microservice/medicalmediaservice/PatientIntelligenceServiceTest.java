package com.esprit.microservice.medicalmediaservice;

import com.esprit.microservice.medicalmediaservice.repositories.DocumentMedicalRepository;
import com.esprit.microservice.medicalmediaservice.repositories.ImageMedicaleRepository;

import com.esprit.microservice.medicalmediaservice.services.PatientIntelligenceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PatientIntelligenceServiceTest {

    @Test
    void testCalculRisk() {

        DocumentMedicalRepository docRepo = mock(DocumentMedicalRepository.class);
        ImageMedicaleRepository imgRepo = mock(ImageMedicaleRepository.class);

        when(docRepo.findByPatientId(1L)).thenReturn(List.of());
        when(imgRepo.findByPatientId(1L)).thenReturn(List.of());

        PatientIntelligenceService service =
                new PatientIntelligenceService(docRepo, imgRepo);

        Map<String, Object> result = service.calculRisk(1L);

        assertEquals("CRITIQUE", result.get("riskLevel"));
    }

    @Test
    void testScoreQualite() {

        DocumentMedicalRepository docRepo = mock(DocumentMedicalRepository.class);
        ImageMedicaleRepository imgRepo = mock(ImageMedicaleRepository.class);

        when(docRepo.findByPatientId(1L)).thenReturn(List.of());

        PatientIntelligenceService service =
                new PatientIntelligenceService(docRepo, imgRepo);

        Map<String, Object> result = service.scoreQualite(1L);

        assertNotNull(result.get("score"));
    }

}