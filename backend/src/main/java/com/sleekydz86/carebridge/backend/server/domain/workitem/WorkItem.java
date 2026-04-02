package com.sleekydz86.carebridge.backend.server.domain.workitem;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkItem(
        UUID id,
        String title,
        String description,
        WorkItemStatus status,
        WorkItemPriority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public WorkItem {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (title.length() > 80) {
            throw new IllegalArgumentException("제목은 80자를 초과할 수 없습니다.");
        }
        description = description == null ? "" : description.trim();
    }

    public WorkItem moveTo(WorkItemStatus nextStatus, LocalDateTime changedAt) {
        return new WorkItem(id, title, description, nextStatus, priority, createdAt, changedAt);
    }
}