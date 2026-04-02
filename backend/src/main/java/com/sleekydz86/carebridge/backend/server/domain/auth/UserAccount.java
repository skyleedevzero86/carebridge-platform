package com.sleekydz86.carebridge.backend.server.domain.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String username,
        String displayName,
        String passwordHash,
        UserRole role,
        LocalDateTime createdAt
) {}