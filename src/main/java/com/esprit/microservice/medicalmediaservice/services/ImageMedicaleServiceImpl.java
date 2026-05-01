package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleResponseDTO;
import com.esprit.microservice.medicalmediaservice.entities.ImageMedicale;
import com.esprit.microservice.medicalmediaservice.repositories.ImageMedicaleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageMedicaleServiceImpl implements IImageMedicaleService {

    private final ImageMedicaleRepository imageMedicaleRepository;

    // ============================================
    // CRUD
    // ============================================

    @Override
    public ImageMedicaleResponseDTO create(ImageMedicaleRequestDTO requestDTO) {
        ImageMedicale image = ImageMedicale.builder()
                .typeImagerie(requestDTO.getTypeImagerie())
                .dateExamen(requestDTO.getDateExamen())
                .description(requestDTO.getDescription())
                .patientId(requestDTO.getPatientId())
                .urlStockage(requestDTO.getUrlStockage())
                .nomFichier(requestDTO.getNomFichier())
                .build();
        return toResponseDTO(imageMedicaleRepository.save(image));
    }

    @Override
    public ImageMedicaleResponseDTO getById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    @Override
    public List<ImageMedicaleResponseDTO> getAll() {
        return imageMedicaleRepository.findAll()
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ImageMedicaleResponseDTO> getByPatientId(Long patientId) {
        return imageMedicaleRepository
                .findByPatientIdOrderByDateExamenDesc(patientId)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ImageMedicaleResponseDTO> getByPatientIdAndType(Long patientId, String type) {
        return imageMedicaleRepository
                .findByPatientIdAndTypeImagerie(patientId, type)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ImageMedicaleResponseDTO update(Long id, ImageMedicaleRequestDTO requestDTO) {
        ImageMedicale image = findEntityById(id);
        image.setTypeImagerie(requestDTO.getTypeImagerie());
        image.setDateExamen(requestDTO.getDateExamen());
        image.setDescription(requestDTO.getDescription());
        image.setPatientId(requestDTO.getPatientId());
        if (requestDTO.getUrlStockage() != null && !requestDTO.getUrlStockage().isBlank()) {
            image.setUrlStockage(requestDTO.getUrlStockage());
        }
        if (requestDTO.getNomFichier() != null && !requestDTO.getNomFichier().isBlank()) {
            image.setNomFichier(requestDTO.getNomFichier());
        }
        return toResponseDTO(imageMedicaleRepository.save(image));
    }

    @Override
    public void delete(Long id) {
        imageMedicaleRepository.delete(findEntityById(id));
    }

    // ============================================
    // COMPARAISON TEMPORELLE
    // ============================================

    @Override
    public Map<String, Object> comparaisonTemporelle(Long patientId) {
        List<ImageMedicale> images = imageMedicaleRepository
                .findByPatientIdOrderByDateExamenDesc(patientId);

        List<ImageMedicale> triees = images.stream()
                .sorted(Comparator.comparing(ImageMedicale::getDateExamen))
                .collect(Collectors.toList());

        List<Map<String, Object>> timeline = triees.stream()
                .map(img -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", img.getId());
                    map.put("typeImagerie", img.getTypeImagerie());
                    map.put("dateExamen", img.getDateExamen().toString());
                    map.put("urlStockage", img.getUrlStockage() != null ? img.getUrlStockage() : "");
                    map.put("description", img.getDescription() != null ? img.getDescription() : "");
                    map.put("nomFichier", img.getNomFichier() != null ? img.getNomFichier() : "");
                    return map;
                })
                .collect(Collectors.toList());

        List<String> typesDisponibles = triees.stream()
                .map(ImageMedicale::getTypeImagerie)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("totalImages", images.size());
        result.put("typesDisponibles", typesDisponibles);
        result.put("timeline", timeline);
        return result;
    }

    // ============================================
    // ANALYSE IA MULTI-IMAGES
    // ============================================

    @Override
    public Map<String, Object> analyseIAComparaison(Long patientId) {
        List<ImageMedicale> images = imageMedicaleRepository
                .findByPatientIdOrderByDateExamenDesc(patientId);

        // Trier par date croissante
        List<ImageMedicale> triees = images.stream()
                .sorted(Comparator.comparing(ImageMedicale::getDateExamen))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();

        if (triees.size() < 2) {
            result.put("global_evolution", "INSUFFISANT");
            result.put("variation_moyenne", 0);
            result.put("variation_max", 0);
            result.put("interpretation", "Pas assez d'images (minimum 2 requises)");
            result.put("comparaisons", Collections.emptyList());
            return result;
        }

        // Récupérer uniquement les URLs HTTP/HTTPS valides (pas de chemins locaux)
        List<String> urls = triees.stream()
                .map(ImageMedicale::getUrlStockage)
                .filter(url -> url != null && !url.isBlank()
                        && (url.startsWith("http://") || url.startsWith("https://")))
                .collect(Collectors.toList());

        if (urls.size() < 2) {
            result.put("global_evolution", "INSUFFISANT");
            result.put("variation_moyenne", 0);
            result.put("variation_max", 0);
            result.put("interpretation", "Pas assez d'images avec URL Cloudinary valide (minimum 2 requises). Ajoutez des images avec une URL https:// valide.");
            result.put("comparaisons", Collections.emptyList());
            return result;
        }

        // Appel IA multi-images
        Map<String, Object> iaResult = appelerIAMulti(urls);

        String globalEvolution = String.valueOf(iaResult.getOrDefault("global_evolution", "STABLE"));
        double variationMoyenne = parseDouble(iaResult.getOrDefault("variation_moyenne", 0));
        double variationMax = parseDouble(iaResult.getOrDefault("variation_max", 0));

        // Interprétation globale
        String interpretation;
        if ("AGGRAVATION".equals(globalEvolution)) {
            interpretation = "⚠️ Dégradation détectée sur la période analysée";
        } else if ("MODIFICATION".equals(globalEvolution)) {
            interpretation = "🔍 Des changements ont été détectés entre les examens";
        } else if ("ERROR".equals(globalEvolution)) {
            interpretation = "❌ Le service d'analyse IA (Python) n'est pas disponible. Démarrez-le avec : uvicorn main:app --port 8000";
        } else {
            interpretation = "✅ État stable sur la période analysée";
        }

        // Enrichir les comparaisons avec les dates
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comparaisons = (List<Map<String, Object>>) iaResult.getOrDefault("comparaisons", Collections.emptyList());

        // Ajouter les dates aux comparaisons
        for (Map<String, Object> comp : comparaisons) {
            int fromIdx = ((Number) comp.getOrDefault("from_index", 0)).intValue();
            int toIdx = ((Number) comp.getOrDefault("to_index", 1)).intValue();
            if (fromIdx < triees.size()) {
                comp.put("from_date", triees.get(fromIdx).getDateExamen().toString());
                comp.put("from_type", triees.get(fromIdx).getTypeImagerie());
            }
            if (toIdx < triees.size()) {
                comp.put("to_date", triees.get(toIdx).getDateExamen().toString());
                comp.put("to_type", triees.get(toIdx).getTypeImagerie());
            }
        }

        result.put("global_evolution", globalEvolution);
        result.put("variation_moyenne", variationMoyenne);
        result.put("variation_max", variationMax);
        result.put("total_comparaisons", iaResult.getOrDefault("total_comparaisons", 0));
        result.put("interpretation", interpretation);
        result.put("comparaisons", comparaisons);

        // Compatibilité rétroactive : première heatmap
        if (!comparaisons.isEmpty()) {
            result.put("heatmap", comparaisons.get(0).get("heatmap"));
            result.put("evolution", comparaisons.get(0).get("evolution"));
            result.put("variation", comparaisons.get(0).get("variation"));
        }

        return result;
    }

    // ============================================
    // APPEL IA — MULTI IMAGES (via URLs JSON)
    // ============================================

    private Map<String, Object> appelerIAMulti(List<String> urls) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("img_urls", urls);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "http://localhost:8000/compare-evolution-multi",
                    request,
                    Map.class
            );

            return response.getBody() != null ? response.getBody() : Collections.emptyMap();

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("global_evolution", "ERROR");
            error.put("variation_moyenne", 0);
            error.put("variation_max", 0);
            error.put("comparaisons", Collections.emptyList());
            return error;
        }
    }

    // ============================================
    // HELPERS
    // ============================================

    private ImageMedicale findEntityById(Long id) {
        return imageMedicaleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ImageMedicale non trouvée avec l'id : " + id));
    }

    private ImageMedicaleResponseDTO toResponseDTO(ImageMedicale image) {
        return ImageMedicaleResponseDTO.builder()
                .id(image.getId())
                .typeImagerie(image.getTypeImagerie())
                .dateExamen(image.getDateExamen())
                .urlStockage(image.getUrlStockage())
                .description(image.getDescription())
                .nomFichier(image.getNomFichier())
                .patientId(image.getPatientId())
                .build();
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}