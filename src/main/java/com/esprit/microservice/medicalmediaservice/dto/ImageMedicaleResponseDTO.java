package com.esprit.microservice.medicalmediaservice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImageMedicaleResponseDTO {
    private Long id;
    private String typeImagerie;
    private LocalDate dateExamen;
    private String urlStockage;
    private String description;
    private String nomFichier;
    private Long patientId;
}