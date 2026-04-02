package com.sleekydz86.carebridge.backend.server.application.workitem;

import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemPriority;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemSortType;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemStatus;
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
