package com.sleekydz86.carebridge.backend.server.domain.workitem;

import java.util.List;

public interface WorkItemSorter {
    WorkItemSortType supports();
    List<WorkItem> sort(List<WorkItem> workItems);
}
