package com.sleekydz86.carebridge.backend.server.application.emr;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationResultJpaRepository extends JpaRepository<ObservationResultEntity, UUID> {
    List<ObservationResultEntity> findByPatientPatientNoOrderByCreatedAtDesc(String patientNo);
    List<ObservationResultEntity> findByOrderOrderNoOrderByCreatedAtDesc(String orderNo);
    long countByPatientPatientNo(String patientNo);
}
