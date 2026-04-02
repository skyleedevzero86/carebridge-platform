package com.sleekydz86.carebridge.backend.server.domain.workitem;

import java.time.LocalDateTime;
import java.util.UUID;

public final class WorkItemFactory {
    private WorkItemFactory()  {}
    public static WorkItem create(String title, String description, WorkItemPriority priority, LocalDateTime now) {
        return new WorkItem(
                UUID.randomUUID(),
                title,
                description,
                WorkItemStatus.BACKLOG,
                priority,
                now,
                now
        );
    }
}