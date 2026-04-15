package com.esprit.microservice.medicalmediaservice.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "document_medical")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentMedical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du fichier est obligatoire")
    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type", nullable = false)
    private String type;

    @NotNull(message = "La date d'upload est obligatoire")
    @Column(name = "date_upload", nullable = false)
    private LocalDate dateUpload;

    @Column(name = "url_stockage")
    private String urlStockage;

    @Column(name = "contenu_ocr", columnDefinition = "TEXT")
    private String contenuOcr;

    @Column(name = "categorie")
    private String categorie;

    @NotNull(message = "L'ID patient est obligatoire")
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    // ✅ NOUVEAU — score de confiance OCR
    @Column(name = "score_confiance")
    private Integer scoreConfiance;

    // ✅ NOUVEAU — niveau de confiance : HAUTE / MOYENNE / FAIBLE
    @Column(name = "niveau_confiance")
    private String niveauConfiance;

    @Column(name = "tags", length = 500)
    private String tags; // stocké comme "URGENT,POST_GREFFE,PEDIATRIQUE"
}