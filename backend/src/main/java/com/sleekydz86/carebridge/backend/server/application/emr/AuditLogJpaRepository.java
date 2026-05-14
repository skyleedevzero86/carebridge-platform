package com.sleekydz86.carebridge.backend.server.application.emr;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {
}
