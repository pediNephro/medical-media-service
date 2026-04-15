package com.esprit.microservice.medicalmediaservice.specifications;



import com.esprit.microservice.medicalmediaservice.entities.DocumentMedical;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DocumentMedicalSpecification {

    public static Specification<DocumentMedical> filter(
            Long patientId,
            String type,
            LocalDate date
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (patientId != null && patientId != 0) {
                predicates.add(cb.equal(root.get("patientId"), patientId));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.like(
                        cb.lower(root.get("type")),
                        "%" + type.toLowerCase() + "%"
                ));
            }

            if (date != null) {
                predicates.add(cb.equal(root.get("dateUpload"), date));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}