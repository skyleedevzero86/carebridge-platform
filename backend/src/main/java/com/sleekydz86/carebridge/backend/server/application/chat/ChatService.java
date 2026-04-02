package com.sleekydz86.carebridge.backend.server.application.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sleekydz86.carebridge.backend.global.config.AppProperties;
import com.sleekydz86.carebridge.backend.global.security.AuthenticatedUserPrincipal;
import com.sleekydz86.carebridge.backend.global.security.InputSanitizer;
import com.sleekydz86.carebridge.backend.server.domain.chat.ChatMessage;
import com.sleekydz86.carebridge.backend.server.domain.chat.ChatMessageFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final AppProperties appProperties;

    public ChatService(ChatMessageJpaRepository chatMessageJpaRepository, AppProperties appProperties) {
        this.chatMessageJpaRepository = chatMessageJpaRepository;
        this.appProperties = appProperties;
    }

    public ChatMessageView append(AuthenticatedUserPrincipal sender, String rawContent) {

        String content = InputSanitizer.sanitize(rawContent, MAX_CONTENT_LENGTH);
        if (content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 내용이 비어있습니다.");
        }

        ChatMessage message = ChatMessageFactory.create(
                sender.userId(),
                sender.displayName(),
                sender.role(),
                content,
                LocalDateTime.now()
        );

        ChatMessageEntity saved = chatMessageJpaRepository.save(toEntity(message));
        return toView(toDomain(saved));
    }


    @Transactional(readOnly = true)
    public List<ChatMessageView> recentMessages(int page) {
        int pageSize = appProperties.pagination().chatPageSize();
        PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "sentAt"));
        List<ChatMessageView> messages = new ArrayList<>(
                chatMessageJpaRepository.findAll(pageable).stream()
                        .map(this::toDomain)
                        .map(this::toView)
                        .toList()
        );
        Collections.reverse(messages);
        return messages;
    }



    private ChatMessageEntity toEntity(ChatMessage message) {
        return new ChatMessageEntity(
                message.id(),
                message.senderId(),
                message.senderName(),
                message.senderRole(),
                message.content(),
                message.sentAt()
        );
    }

    private ChatMessage toDomain(ChatMessageEntity entity) {
        return new ChatMessage(
                entity.getId(),
                entity.getSenderId(),
                entity.getSenderName(),
                entity.getSenderRole(),
                entity.getContent(),
                entity.getSentAt()
        );
    }

    private ChatMessageView toView(ChatMessage message) {
        return new ChatMessageView(
                message.id().toString(),
                message.senderId().toString(),
                message.senderName(),
                message.senderRole().name(),
                message.content(),
                message.sentAt()
        );
    }

    public record ChatMessageView(
            String id,
            String senderId,
            String senderName,
            String senderRole,
            String content,
            LocalDateTime sentAt
    ) {}
}