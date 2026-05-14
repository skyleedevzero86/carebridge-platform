package com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamOrderJpaRepository extends JpaRepository<ExamOrderEntity, UUID> {
    Optional<ExamOrderEntity> findByOrderNo(String orderNo);
    List<ExamOrderEntity> findByPatientPatientNoOrderByOrderedAtDesc(String patientNo);
    List<ExamOrderEntity> findAllByOrderByOrderedAtDesc();
}

