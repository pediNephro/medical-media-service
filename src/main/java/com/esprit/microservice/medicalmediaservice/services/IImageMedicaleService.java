package com.esprit.microservice.medicalmediaservice.services;

import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleRequestDTO;
import com.esprit.microservice.medicalmediaservice.dto.ImageMedicaleResponseDTO;
import java.util.List;
import java.util.Map;

public interface IImageMedicaleService {
    ImageMedicaleResponseDTO create(ImageMedicaleRequestDTO requestDTO);
    ImageMedicaleResponseDTO getById(Long id);
    List<ImageMedicaleResponseDTO> getAll();
    List<ImageMedicaleResponseDTO> getByPatientId(Long patientId);
    List<ImageMedicaleResponseDTO> getByPatientIdAndType(Long patientId, String type);
    ImageMedicaleResponseDTO update(Long id, ImageMedicaleRequestDTO requestDTO);


    void delete(Long id);

    // ✅ FONCTION 3 : COMPARAISON TEMPORELLE
    Map<String, Object> comparaisonTemporelle(Long patientId);

    Map<String, Object> analyseIAComparaison(Long patientId);
}