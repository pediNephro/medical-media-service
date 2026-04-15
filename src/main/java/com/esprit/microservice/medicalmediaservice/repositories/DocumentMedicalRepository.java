package com.esprit.microservice.medicalmediaservice.repositories;

import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentMedicalRepository
        extends JpaRepository<DocumentMedical, Long>,
        JpaSpecificationExecutor<DocumentMedical> {

    List<DocumentMedical> findByPatientId(Long patientId);

    List<DocumentMedical> findByPatientIdAndType(Long patientId, String type);

    List<DocumentMedical> findByPatientIdOrderByDateUploadDesc(Long patientId);

    List<DocumentMedical> findByContenuOcrContainingIgnoreCase(String keyword);
}