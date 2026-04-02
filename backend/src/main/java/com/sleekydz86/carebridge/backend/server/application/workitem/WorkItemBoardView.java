package com.sleekydz86.carebridge.backend.server.application.workitem;

import java.time.LocalDateTime;
import java.util.List;

public record WorkItemBoardView(
        WorkItemSortType sortBy,
        int totalCount,
        List<ItemView> items,
        boolean cached
) {
    public record ItemView(
            String id,
            String title,
            String description,
            WorkItemStatus status,
            WorkItemPriority priority,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
