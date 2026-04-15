package com.esprit.microservice.medicalmediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentMedicalRequestDTO {

    @NotBlank(message = "Le nom du fichier est obligatoire")
    private String nomFichier;

    private String type;

    @NotNull(message = "La date d'upload est obligatoire")
    private LocalDate dateUpload;

    private String categorie;
    private String contenuOcr;
    private String urlStockage;

    @NotNull(message = "L'ID patient est obligatoire")
    private Long patientId;
}