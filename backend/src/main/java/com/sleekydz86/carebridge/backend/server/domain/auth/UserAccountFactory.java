package com.sleekydz86.carebridge.backend.server.domain.auth;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public final class UserAccountFactory {

    private UserAccountFactory()  {}

    public static UserAccount create(String username, String displayName, String passwordHash, UserRole role, LocalDateTime createdAt) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();

        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }

        if (normalizedDisplayName.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해 주세요.");
        }

        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        return new UserAccount(UUID.randomUUID(), normalizedUsername, normalizedDisplayName, passwordHash, role, createdAt);
    }
}