package com.esprit.microservice.medicalmediaservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "image_medicale")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ImageMedicale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le type d'imagerie est obligatoire")
    @Column(name = "type_imagerie", nullable = false)
    private String typeImagerie;

    @NotNull(message = "La date d'examen est obligatoire")
    @Column(name = "date_examen", nullable = false)
    private LocalDate dateExamen;

    @Column(name = "url_stockage")
    private String urlStockage;

    @Column(name = "public_id_cloudinary")
    private String publicIdCloudinary;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @NotNull(message = "L'ID patient est obligatoire")
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
}