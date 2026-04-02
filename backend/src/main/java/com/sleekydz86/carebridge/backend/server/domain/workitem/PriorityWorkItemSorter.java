package com.sleekydz86.carebridge.backend.server.domain.workitem;

import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;

@Component
public class PriorityWorkItemSorter implements WorkItemSorter {

    @Override
    public WorkItemSortType supports() {
        return WorkItemSortType.PRIORITY;
    }

    @Override
    public List<WorkItem> sort(List<WorkItem> workItems) {
        return workItems.stream()
                .sorted(Comparator
                        .comparing(WorkItem::priority, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(WorkItem::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
