package com.sleekydz86.carebridge.backend.server.application.port.out;

public interface AuditLogPort {
    void save(String action, String targetType, String targetId, String createdBy);
}

