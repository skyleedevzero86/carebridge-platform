package com.sleekydz86.carebridge.backend.server.application.chat;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageEntity, UUID> {

    Page<ChatMessageEntity> findAll(Pageable pageable);

}