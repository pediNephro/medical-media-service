package com.esprit.microservice.medicalmediaservice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentMedicalResponseDTO {
    private Long id;
    private String nomFichier;
    private String type;
    private LocalDate dateUpload;
    private String urlStockage;
    private String contenuOcr;
    private String categorie;
    private Long patientId;

    // ✅ NOUVEAU
    private Integer scoreConfiance;
    private String niveauConfiance;

}