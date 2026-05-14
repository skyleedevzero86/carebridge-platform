package com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientJpaRepository extends JpaRepository<PatientEntity, UUID> {
    Optional<PatientEntity> findByPatientNo(String patientNo);
}

