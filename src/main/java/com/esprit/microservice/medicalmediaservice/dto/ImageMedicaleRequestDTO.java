package com.esprit.microservice.medicalmediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImageMedicaleRequestDTO {

    @NotBlank
    private String typeImagerie;

    @NotNull
    private LocalDate dateExamen;

    private String description;

    private String urlStockage;   // ✅ IMPORTANT
    private String nomFichier;    // ✅ IMPORTANT

    @NotNull
    private Long patientId;
}