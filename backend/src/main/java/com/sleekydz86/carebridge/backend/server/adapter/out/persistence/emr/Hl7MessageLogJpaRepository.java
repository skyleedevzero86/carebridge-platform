package com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Hl7MessageLogJpaRepository extends JpaRepository<Hl7MessageLogEntity, UUID> {
    Optional<Hl7MessageLogEntity> findByMessageControlId(String messageControlId);
    List<Hl7MessageLogEntity> findTop50ByOrderByReceivedAtDesc();
}

