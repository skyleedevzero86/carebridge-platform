package com.sleekydz86.carebridge.backend.server.domain.chat;

import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ChatMessageFactory {

    private ChatMessageFactory()  {}

    public static ChatMessage create(UUID senderId, String senderName, UserRole senderRole, String content, LocalDateTime sentAt) {
        String normalizedContent = content == null ? "" : content.trim();
        String normalizedSenderName = senderName == null ? "" : senderName.trim();

        if (normalizedSenderName.isBlank()) {
            throw new IllegalArgumentException("메시지 발신자 이름이 비어 있습니다.");
        }

        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("채팅 내용을 입력해 주세요.");
        }

        if (normalizedContent.length() > 500) {
            throw new IllegalArgumentException("채팅 내용은 500자 이하만 가능합니다.");
        }

        return new ChatMessage(UUID.randomUUID(), senderId, normalizedSenderName, senderRole, normalizedContent, sentAt);
    }
}