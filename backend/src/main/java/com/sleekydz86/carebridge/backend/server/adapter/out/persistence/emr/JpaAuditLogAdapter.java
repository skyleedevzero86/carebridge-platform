package com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr;

import java.time.LocalDateTime;
import com.sleekydz86.carebridge.backend.server.application.port.out.AuditLogPort;
import org.springframework.stereotype.Component;

@Component
public class JpaAuditLogAdapter implements AuditLogPort {
    private final AuditLogJpaRepository auditLogJpaRepository;

    public JpaAuditLogAdapter(AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Override
    public void save(String action, String targetType, String targetId, String createdBy) {
        auditLogJpaRepository.save(new AuditLogEntity(null, action, targetType, targetId, createdBy, LocalDateTime.now()));
    }
}
