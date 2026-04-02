package com.sleekydz86.carebridge.backend.server.domain.chat;

import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessage(
        UUID id,
        UUID senderId,
        String senderName,
        UserRole senderRole,
        String content,
        LocalDateTime sentAt
) {}