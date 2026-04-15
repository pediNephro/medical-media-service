package com.esprit.microservice.medicalmediaservice;

import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleRequestDTO;
import com.esprit.microservice.medicalmediaservice.entities.ImageMedicale;
import com.esprit.microservice.medicalmediaservice.repositories.ImageMedicaleRepository;

import com.esprit.microservice.medicalmediaservice.services.ImageMedicaleServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ImageMedicaleServiceImplTest {

    private final ImageMedicaleRepository repo = mock(ImageMedicaleRepository.class);
    private final ImageMedicaleServiceImpl service = new ImageMedicaleServiceImpl(repo);

    @Test
    void testCreateImage() {

        ImageMedicale saved = ImageMedicale.builder()
                .id(1L)
                .typeImagerie("IRM")
                .dateExamen(LocalDate.now())
                .patientId(1L)
                .build();

        when(repo.save(any())).thenReturn(saved);

        ImageMedicaleRequestDTO dto = ImageMedicaleRequestDTO.builder()
                .typeImagerie("IRM")
                .dateExamen(LocalDate.now())
                .patientId(1L)
                .build();

        var result = service.create(dto);

        assertNotNull(result);
        assertEquals("IRM", result.getTypeImagerie());
    }
}