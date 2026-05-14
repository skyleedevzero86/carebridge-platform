package com.sleekydz86.carebridge.backend.server.application.emr;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 80)
    private String targetType;
    @Column(nullable = false, length = 80)
    private String targetId;
    @Column(nullable = false, length = 80)
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AuditLogEntity() {}

    public AuditLogEntity(UUID id, String action, String targetType, String targetId, String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
