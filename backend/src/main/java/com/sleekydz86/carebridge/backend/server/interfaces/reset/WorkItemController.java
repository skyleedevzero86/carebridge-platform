package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import com.sleekydz86.carebridge.backend.server.application.workitem.WorkItemBoardView;
import com.sleekydz86.carebridge.backend.server.application.workitem.WorkItemService;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemPriority;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemSortType;
import com.sleekydz86.carebridge.backend.server.domain.workitem.WorkItemStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-items")
public class WorkItemController {

    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @PostMapping
    public ResponseEntity<WorkItemBoardView.ItemView> create(@Valid @RequestBody CreateWorkItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workItemService.create(request.title(), request.description(), request.priority()));
    }

    @GetMapping
    public ResponseEntity<WorkItemBoardView> list(@RequestParam(defaultValue = "RECENT") String sortBy) {
        return ResponseEntity.ok(workItemService.list(WorkItemSortType.valueOf(sortBy.toUpperCase())));
    }

    @PatchMapping("/{workItemId}/status")
    public ResponseEntity<WorkItemBoardView.ItemView> updateStatus(
            @PathVariable String workItemId,
            @Valid @RequestBody UpdateWorkItemStatusRequest request
    ) {
        return ResponseEntity.ok(workItemService.updateStatus(workItemId, request.status()));
    }

    public record CreateWorkItemRequest(
            @NotBlank @Size(max = 80) String title,
            @Size(max = 1000) String description,
            @NotNull WorkItemPriority priority
    ) {}
    public record UpdateWorkItemStatusRequest(@NotNull WorkItemStatus status)  {}
}
