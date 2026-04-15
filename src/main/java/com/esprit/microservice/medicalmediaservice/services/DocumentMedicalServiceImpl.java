package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.DocumentMedicalResponseDTO;
import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import com.esprit.microservice.medicalmediaservice.repositories.DocumentMedicalRepository;
import com.esprit.microservice.medicalmediaservice.specifications.DocumentMedicalSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentMedicalServiceImpl implements IDocumentMedicalService {

    private final DocumentMedicalRepository documentMedicalRepository;

    private static final List<String> TYPES_REQUIS = List.of(
            "ORDONNANCE", "COMPTE_RENDU", "BILAN", "CONSENTEMENT", "RADIO"
    );

    // =========================================================
    // CRUD DE BASE
    // =========================================================

    @Override
    public DocumentMedicalResponseDTO create(DocumentMedicalRequestDTO requestDTO) {
        DocumentMedical document = toEntity(requestDTO);

        if (requestDTO.getContenuOcr() != null && !requestDTO.getContenuOcr().isEmpty()) {
            Map<String, Object> classification = classifyWithConfidence(requestDTO.getContenuOcr());
            document.setScoreConfiance((Integer) classification.get("score"));
            document.setNiveauConfiance((String) classification.get("confiance"));
            // Ne pas écraser le type si l'utilisateur en a choisi un
            if (requestDTO.getType() == null || requestDTO.getType().isEmpty()) {
                document.setType((String) classification.get("type"));

            } else {
                document.setType(requestDTO.getType());
            }
        } else {
            document.setType(requestDTO.getType() != null ? requestDTO.getType() : "AUTRE");
            document.setScoreConfiance(0);
            document.setNiveauConfiance("FAIBLE");
        }
        return toResponseDTO(documentMedicalRepository.save(document));
    }

    @Override
    public DocumentMedicalResponseDTO getById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    @Override
    public List<DocumentMedicalResponseDTO> getAll() {
        return documentMedicalRepository.findAll()
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentMedicalResponseDTO> getByPatientId(Long patientId) {
        return documentMedicalRepository
                .findByPatientIdOrderByDateUploadDesc(patientId)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentMedicalResponseDTO> getByPatientIdAndType(Long patientId, String type) {
        return documentMedicalRepository
                .findByPatientIdAndType(patientId, type)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentMedicalResponseDTO update(Long id, DocumentMedicalRequestDTO requestDTO) {
        DocumentMedical document = findEntityById(id);

        document.setNomFichier(requestDTO.getNomFichier());
        document.setDateUpload(requestDTO.getDateUpload());
        document.setCategorie(requestDTO.getCategorie());
        document.setPatientId(requestDTO.getPatientId());

        if (requestDTO.getUrlStockage() != null)
            document.setUrlStockage(requestDTO.getUrlStockage());
        // Dans update(), ajouter avant le bloc if (requestDTO.getContenuOcr()) :
        if (requestDTO.getType() != null && !requestDTO.getType().isEmpty()) {
            document.setType(requestDTO.getType());
        }

        if (requestDTO.getContenuOcr() != null) {
            document.setContenuOcr(requestDTO.getContenuOcr());
            Map<String, Object> classification = classifyWithConfidence(requestDTO.getContenuOcr());
            document.setType((String) classification.get("type"));
            document.setScoreConfiance((Integer) classification.get("score"));
            document.setNiveauConfiance((String) classification.get("confiance"));
        }

        return toResponseDTO(documentMedicalRepository.save(document));
    }

    @Override
    public void delete(Long id) {
        documentMedicalRepository.delete(findEntityById(id));
    }

    // =========================================================
    // FONCTION OCR
    // =========================================================

    @Override
    public DocumentMedicalResponseDTO extractOcr(Long id, String contenuOcr) {
        DocumentMedical document = findEntityById(id);
        document.setContenuOcr(contenuOcr);

        if (contenuOcr != null && !contenuOcr.isEmpty()) {
            Map<String, Object> classification = classifyWithConfidence(contenuOcr);
            document.setType((String) classification.get("type"));
            document.setScoreConfiance((Integer) classification.get("score"));
            document.setNiveauConfiance((String) classification.get("confiance"));
        }

        return toResponseDTO(documentMedicalRepository.save(document));
    }

    // =========================================================
    // FONCTION COMPLÉTUDE DOSSIER
    // =========================================================

    @Override
    public Map<String, Object> verifierCompletude(Long patientId) {
        List<DocumentMedical> docs = documentMedicalRepository
                .findByPatientIdOrderByDateUploadDesc(patientId);

        // Utiliser le champ type directement (plus fiable que le texte OCR)
        List<String> presents = docs.stream()
                .map(DocumentMedical::getType)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> manquants = new ArrayList<>(TYPES_REQUIS);
        manquants.removeAll(presents);

        int total = TYPES_REQUIS.size();
        int ok = (int) presents.stream().filter(TYPES_REQUIS::contains).count();
        int pourcentage = total == 0 ? 0 : (int) ((ok * 100.0) / total);

        String score;
        if (pourcentage < 30) score = "CRITIQUE";
        else if (pourcentage < 70) score = "MOYEN";
        else score = "BON";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("complet", manquants.isEmpty());
        result.put("pourcentage", pourcentage);
        result.put("score", score);
        result.put("typesRequis", TYPES_REQUIS);
        result.put("presents", presents);
        result.put("manquants", manquants);
        result.put("totalDocuments", docs.size());
        return result;
    }

    // =========================================================
    // RECHERCHE OCR
    // =========================================================

    @Override
    public List<DocumentMedicalResponseDTO> searchByOcr(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return documentMedicalRepository.findAll()
                    .stream().map(this::toResponseDTO)
                    .collect(Collectors.toList());
        }
        return documentMedicalRepository
                .findByContenuOcrContainingIgnoreCase(keyword)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // FILTRE PAGINÉ
    // =========================================================

    @Override
    public Page<DocumentMedicalResponseDTO> filter(Long patientId, String type,
                                                   LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentMedicalRepository.findAll(
                DocumentMedicalSpecification.filter(patientId, type, date), pageable
        ).map(this::toResponseDTO);
    }

    // =========================================================
    // ✅ NOUVEAU — SCORE DE CONFIANCE OCR
    // =========================================================

    @Override
    public Map<String, Object> classifyWithConfidence(String text) {

        if (text == null || text.trim().isEmpty()) {
            return Map.of(
                    "type", "AUTRE",
                    "score", 0,
                    "confiance", "FAIBLE"
            );
        }

        // 🔥 NORMALISATION PRO (TRÈS IMPORTANT)
        String t = text.toLowerCase()
                .replace(",", ".")          // 1,2 → 1.2
                .replaceAll("\\s+", " ")    // nettoyer espaces
                .trim();

        Map<String, Integer> scores = new LinkedHashMap<>();

        // ================================
        // 📄 ORDONNANCE
        // ================================
        scores.put("ORDONNANCE", scoreKeywords(t, Map.of(
                "ordonnance", 60,
                "posologie", 25,
                "medicament", 20,
                "comprimes", 15,
                "mg", 10,
                "dose", 10,
                "prescription", 20
        )));

        // ================================
        // 🧪 BILAN BIOLOGIQUE
        // ================================
        scores.put("BILAN", scoreKeywords(t, Map.of(
                "bilan", 60,
                "nfs", 30,
                "creatinine", 25,
                "urée", 25,
                "glycemie", 25,
                "hemoglobine", 20,
                "mmol", 15,
                "analyse", 15,
                "laboratoire", 20
        )));

        // ================================
        // 🩻 RADIO / IMAGERIE
        // ================================
        scores.put("RADIO", scoreKeywords(t, Map.of(
                "radio", 50,
                "scanner", 40,
                "irm", 40,
                "radiographie", 35,
                "echographie", 30,
                "cliche", 20,
                "imagerie", 25
        )));

        // ================================
        // 📑 COMPTE RENDU
        // ================================
        scores.put("COMPTE_RENDU", scoreKeywords(t, Map.of(
                "compte rendu", 60,
                "diagnostic", 30,
                "conclusion", 25,
                "examen", 15,
                "consultation", 20,
                "observation", 15,
                "rapport", 20,
                "synthese", 25
        )));

        // ================================
        // 📝 CONSENTEMENT
        // ================================
        scores.put("CONSENTEMENT", scoreKeywords(t, Map.of(
                "consentement", 70,
                "signature", 25,
                "accord", 20,
                "intervention", 15,
                "autorisation", 25,
                "patient accepte", 30
        )));

        // ================================
        // 🔍 CHOIX DU MEILLEUR TYPE
        // ================================
        String bestType = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("AUTRE");

        int bestScore = scores.getOrDefault(bestType, 0);

        // ================================
        // ⚠️ SEUIL MINIMUM
        // ================================
        if (bestScore < 15) {
            return new LinkedHashMap<>(Map.of(
                    "type", "AUTRE",
                    "score", 0,
                    "confiance", "FAIBLE",
                    "scores", scores
            ));
        }

        // ================================
        // 🎯 NIVEAU DE CONFIANCE
        // ================================
        String confiance;
        if (bestScore >= 60) confiance = "HAUTE";
        else if (bestScore >= 30) confiance = "MOYENNE";
        else confiance = "FAIBLE";

        int scoreFinal = Math.min(bestScore, 100);

        // ================================
        // 📦 RESULTAT FINAL
        // ================================
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", bestType);
        result.put("score", scoreFinal);
        result.put("confiance", confiance);
        result.put("scores", scores);
        result.put("scores", scores);

        return result;
    }

    private int scoreKeywords(String text, Map<String, Integer> keywords) {
        return keywords.entrySet().stream()
                .filter(e -> text.contains(e.getKey()))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    // =========================================================
    // ✅ NOUVEAU — DASHBOARD STATISTIQUES
    // =========================================================

    @Override
    public Map<String, Object> getDashboardStats() {
        List<DocumentMedical> tous = documentMedicalRepository.findAll();

        // Stats par type
        Map<String, Long> parType = tous.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getType() != null ? d.getType() : "AUTRE",
                        Collectors.counting()
                ));

        // Stats par mois (6 derniers mois)
        Map<String, Long> parMois = tous.stream()
                .filter(d -> d.getDateUpload() != null)
                .filter(d -> d.getDateUpload().isAfter(LocalDate.now().minusMonths(6)))
                .collect(Collectors.groupingBy(
                        d -> d.getDateUpload().getYear() + "-"
                                + String.format("%02d", d.getDateUpload().getMonthValue()),
                        Collectors.counting()
                ));

        // Patients distincts
        List<Long> patientIds = tous.stream()
                .map(DocumentMedical::getPatientId)
                .distinct()
                .toList();

        // Patients avec dossier incomplet
        long incomplets = patientIds.stream()
                .filter(pid -> {
                    List<String> types = tous.stream()
                            .filter(d -> pid.equals(d.getPatientId()))
                            .map(DocumentMedical::getType)
                            .filter(Objects::nonNull)
                            .distinct().toList();
                    return !types.containsAll(TYPES_REQUIS);
                }).count();

        // OCR et confiance
        long avecOcr = tous.stream()
                .filter(d -> d.getContenuOcr() != null && !d.getContenuOcr().isEmpty())
                .count();

        long hauteConfiance = tous.stream()
                .filter(d -> "HAUTE".equals(d.getNiveauConfiance()))
                .count();

        long moyenneConfiance = tous.stream()
                .filter(d -> "MOYENNE".equals(d.getNiveauConfiance()))
                .count();

        long faibleConfiance = tous.stream()
                .filter(d -> "FAIBLE".equals(d.getNiveauConfiance()))
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDocuments", tous.size());
        stats.put("totalPatients", patientIds.size());
        stats.put("patientsIncomplets", incomplets);
        stats.put("tauxCompletion", patientIds.isEmpty() ? 0 :
                Math.round((double) (patientIds.size() - incomplets) / patientIds.size() * 100));
        stats.put("parType", parType);
        stats.put("parMois", new TreeMap<>(parMois));
        stats.put("avecOcr", avecOcr);
        stats.put("tauxOcr", tous.isEmpty() ? 0 :
                Math.round((double) avecOcr / tous.size() * 100));
        stats.put("hauteConfiance", hauteConfiance);
        stats.put("moyenneConfiance", moyenneConfiance);
        stats.put("faibleConfiance", faibleConfiance);
        return stats;
    }

    // =========================================================
    // HELPERS PRIVÉS
    // =========================================================

    private DocumentMedical toEntity(DocumentMedicalRequestDTO dto) {
        return DocumentMedical.builder()
                .nomFichier(dto.getNomFichier())
                .type(null)
                .dateUpload(dto.getDateUpload())
                .categorie(dto.getCategorie())
                .urlStockage(dto.getUrlStockage())
                .contenuOcr(dto.getContenuOcr())
                .patientId(dto.getPatientId())
                .build();
    }

    private DocumentMedical findEntityById(Long id) {
        return documentMedicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DocumentMedical non trouvé avec l'id : " + id));
    }

    private DocumentMedicalResponseDTO toResponseDTO(DocumentMedical document) {
        return DocumentMedicalResponseDTO.builder()
                .id(document.getId())
                .nomFichier(document.getNomFichier())
                .type(document.getType())
                .dateUpload(document.getDateUpload())
                .urlStockage(document.getUrlStockage())
                .contenuOcr(document.getContenuOcr())
                .categorie(document.getCategorie())
                .patientId(document.getPatientId())
                .scoreConfiance(document.getScoreConfiance())
                .niveauConfiance(document.getNiveauConfiance())
                .build();
    }
    @Override
    public Map<String, Object> analyserRisqueRenal(String texte) {

        if (texte == null || texte.isEmpty()) {
            return Map.of("risk", "FAIBLE");
        }

        // 🔥 normalisation (IMPORTANT)
        String t = normalize(texte);

        double creatinine = extractValueSafe(t, "creatinine");
        double uree = extractValueSafe(t, "uree");
        double potassium = extractValueSafe(t, "potassium");

        System.out.println("creatinine=" + creatinine);
        System.out.println("uree=" + uree);
        System.out.println("potassium=" + potassium);

        // 🔥 LOGIQUE CORRECTE (PRIORITÉ CRITIQUE)
        if (creatinine >= 140 || uree >= 10 || potassium >= 5.5) {
            return Map.of("risk", "CRITIQUE");
        }

        if (creatinine >= 100 || uree >= 7) {
            return Map.of("risk", "MODERE");
        }

        return Map.of("risk", "FAIBLE");
    }
    private String extractValue(String text, String keyword) {
        String pattern = keyword + "\\s*[:=]?\\s*(\\d+\\.?\\d*)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);

        if (m.find()) return m.group(1);
        return null;
    }@Override
    public List<Map<String, Object>> getTimeline(Long patientId) {
        List<DocumentMedical> docs = documentMedicalRepository
                .findByPatientIdOrderByDateUploadDesc(patientId);

        List<Map<String, Object>> timeline = new ArrayList<>();

        for (DocumentMedical doc : docs) {
            Map<String, Object> event = new LinkedHashMap<>();

            event.put("date", doc.getDateUpload());
            event.put("type", doc.getType());

            if (doc.getContenuOcr() != null) {
                Map<String, Object> risk = analyserRisqueRenal(doc.getContenuOcr());
                event.put("risk", risk.get("risk"));
                event.put("score", risk.get("score"));
            }

            timeline.add(event);
        }

        return timeline;
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