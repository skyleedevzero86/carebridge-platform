package com.sleekydz86.carebridge.backend.server.interfaces.rest;

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
import org.springframework.web.server.ResponseStatusException;
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
        WorkItemSortType sortType;
        try {
            sortType = WorkItemSortType.valueOf(sortBy.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ì§€?í•˜ì§€ ?ŠëŠ” ?•ë ¬ ë°©ì‹?…ë‹ˆ??");
        }
        return ResponseEntity.ok(workItemService.list(sortType));
    }

    @PatchMapping("/{workItemId}/status")
    public ResponseEntity<WorkItemBoardView.ItemView> updateStatus(
            @PathVariable String workItemId,
            @Valid @RequestBody UpdateWorkItemStatusRequest request
    ) {
        return ResponseEntity.ok(workItemService.updateStatus(workItemId, request.status()));
    }

    public record CreateWorkItemRequest(
            @NotBlank(message = "?œëª©???…ë ¥??ì£¼ì„¸??")
            @Size(max = 80, message = "?œëª©?€ 80???´í•˜?¬ì•¼ ?©ë‹ˆ??") String title,
            @Size(max = 1000, message = "?¤ëª…?€ 1000???´í•˜?¬ì•¼ ?©ë‹ˆ??") String description,
            @NotNull(message = "?°ì„ ?œìœ„ë¥?? íƒ??ì£¼ì„¸??") WorkItemPriority priority
    ) {}
    public record UpdateWorkItemStatusRequest(@NotNull(message = "?íƒœë¥?? íƒ??ì£¼ì„¸??") WorkItemStatus status)  {}
}
