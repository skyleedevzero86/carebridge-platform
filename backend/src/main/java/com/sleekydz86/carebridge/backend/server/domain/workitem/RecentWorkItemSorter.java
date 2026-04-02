package com.sleekydz86.carebridge.backend.server.domain.workitem;

import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class RecentWorkItemSorter implements WorkItemSorter {

    @Override
    public WorkItemSortType supports() {
        return WorkItemSortType.RECENT;
    }

    @Override
    public List<WorkItem> sort(List<WorkItem> workItems) {
        return workItems.stream()
                .sorted(Comparator.comparing(
                        WorkItem::updatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }
}
