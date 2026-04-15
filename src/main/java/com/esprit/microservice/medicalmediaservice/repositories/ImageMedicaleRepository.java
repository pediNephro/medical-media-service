package com.esprit.microservice.medicalmediaservice.repositories;

import com.esprit.microservice.medicalmediaservice.entities.ImageMedicale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ImageMedicaleRepository extends JpaRepository<ImageMedicale, Long> {
    List<ImageMedicale> findByPatientId(Long patientId);
    List<ImageMedicale> findByPatientIdAndTypeImagerie(Long patientId, String typeImagerie);
    List<ImageMedicale> findByPatientIdOrderByDateExamenDesc(Long patientId);
}