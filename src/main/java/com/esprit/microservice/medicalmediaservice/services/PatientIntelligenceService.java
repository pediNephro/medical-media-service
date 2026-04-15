package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import com.esprit.microservice.medicalmediaservice.entities.ImageMedicale;
import com.esprit.microservice.medicalmediaservice.repositories.DocumentMedicalRepository;
import com.esprit.microservice.medicalmediaservice.repositories.ImageMedicaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PatientIntelligenceService {

    private final DocumentMedicalRepository documentRepo;
    private final ImageMedicaleRepository imageRepo;

    // =========================================================
    // 1. 🔥 RISK ENGINE
    // =========================================================
    public Map<String, Object> calculRisk(Long patientId) {

        List<DocumentMedical> docs = documentRepo.findByPatientId(patientId);
        List<ImageMedicale> images = imageRepo.findByPatientId(patientId);

        int score = 0;
        List<String> facteurs = new ArrayList<>();

        if (docs.size() < 3) {
            score += 40;
            facteurs.add("Peu de documents");
        }

        if (images.isEmpty()) {
            score += 40;
            facteurs.add("Aucune imagerie");
        }

        long faible = docs.stream()
                .filter(d -> "FAIBLE".equals(d.getNiveauConfiance()))
                .count();

        if (faible > 0) {
            score += 20;
            facteurs.add("OCR faible");
        }

        // 🔥 CORRECTION ICI
        String level = score >= 60 ? "CRITIQUE"
                : score >= 30 ? "MOYEN"
                : "FAIBLE";

        return Map.of(
                "score", score,
                "riskLevel", level,
                "facteurs", facteurs
        );
    }
    // =========================================================
    // 2. 🤖 ASSISTANT MÉDICAL
    // =========================================================
    public Map<String, Object> assistantMedical(Long patientId) {

        List<DocumentMedical> docs = documentRepo.findByPatientId(patientId);
        List<ImageMedicale> images = imageRepo.findByPatientId(patientId);

        String resume = "Patient avec " + docs.size() + " documents et "
                + images.size() + " examens.";

        List<String> alertes = new ArrayList<>();

        docs.forEach(d -> {
            if ("FAIBLE".equals(d.getNiveauConfiance()))
                alertes.add("Document incertain : " + d.getNomFichier());
        });

        String reco;
        if (images.isEmpty()) reco = "Ajouter une imagerie";
        else reco = "Suivi normal";

        return Map.of(
                "resume", resume,
                "alertes", alertes,
                "recommandation", reco
        );
    }

    // =========================================================
    // 3. 📊 SCORE QUALITÉ DOSSIER
    // =========================================================
    public Map<String, Object> scoreQualite(Long patientId) {

        List<DocumentMedical> docs = documentRepo.findByPatientId(patientId);

        int score = 100;

        if (docs.size() < 3) score -= 30;

        long sansOcr = docs.stream()
                .filter(d -> d.getContenuOcr() == null)
                .count();

        score -= sansOcr * 10;

        String niveau = score > 70 ? "BON"
                : score > 40 ? "MOYEN"
                : "FAIBLE";

        return Map.of(
                "score", score,
                "niveau", niveau
        );
    }

    // =========================================================
    // 4. 🚨 PRIORITÉ PATIENT
    // =========================================================
    public Map<String, Object> priorite(Long patientId) {

        Map<String, Object> risk = calculRisk(patientId);

        int score = (int) risk.get("score");

        String priorite = score > 70 ? "URGENT"
                : score > 40 ? "NORMAL"
                : "BAS";

        return Map.of(
                "priorite", priorite,
                "raison", risk.get("facteurs")
        );
    }

    public Map<String, Object> analyserRisqueRenal(String texte) {

        if (texte == null || texte.isEmpty()) {
            return Map.of("risk", "FAIBLE");
        }

        // 🔥 NORMALISATION (clé du fix)
        String t = normalize(texte);

        double creatinine = extractValueSafe(t, "creatinine");
        double uree = extractValueSafe(t, "uree");
        double potassium = extractValueSafe(t, "potassium");

        System.out.println("creatinine=" + creatinine);
        System.out.println("uree=" + uree);
        System.out.println("potassium=" + potassium);

        if (creatinine >= 140 || uree >= 10 || potassium >= 5.5) {
            return Map.of("risk", "CRITIQUE");
        }

        if (creatinine >= 100 || uree >= 7) {
            return Map.of("risk", "MODERE");
        }

        return Map.of("risk", "FAIBLE");
    }
    private double extractValueSafe(String text, String keyword) {

        try {
            String regex = keyword + "\\s*(\\d+(\\.\\d+)?)";

            java.util.regex.Pattern pattern =
                    java.util.regex.Pattern.compile(regex);

            java.util.regex.Matcher matcher =
                    pattern.matcher(text);

            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }

        } catch (Exception e) {
            return 0;
        }

        return 0;
    }
    private String normalize(String input) {

        String normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);

        return normalized
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase();
    }
}