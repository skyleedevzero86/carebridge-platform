package com.sleekydz86.carebridge.backend.server.application.workitem;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.carebridge.backend.server.domain.workitem.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class WorkItemService {

    private static final String CACHE_PREFIX = "carebridge:work-items:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final WorkItemJpaRepository repository;
    private final List<WorkItemSorter> sorters;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public WorkItemService(
            WorkItemJpaRepository repository,
            List<WorkItemSorter> sorters,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.sorters = sorters;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public WorkItemBoardView.ItemView create(String title, String description, WorkItemPriority priority) {
        WorkItem saved = saveDomain(WorkItemFactory.create(title, description, priority, LocalDateTime.now()));
        clearCache();
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public WorkItemBoardView list(WorkItemSortType sortType) {
        try {
            String cacheKey = CACHE_PREFIX + sortType.name().toLowerCase();
            String cachedPayload = null;
            try {
                cachedPayload = redisTemplate.opsForValue().get(cacheKey);
            } catch (RuntimeException ignored) {
            }

            if (cachedPayload != null && !cachedPayload.isBlank()) {
                try {
                    WorkItemBoardView cached = objectMapper.readValue(cachedPayload, WorkItemBoardView.class);
                    return new WorkItemBoardView(cached.sortBy(), cached.totalCount(), cached.items(), true);
                } catch (JsonProcessingException ignored) {
                    try {
                        redisTemplate.delete(cacheKey);
                    } catch (RuntimeException ignoredDeleteFailure) {
                    }
                }
            }

            WorkItemSorter sorter = sorters.stream()
                    .filter(candidate -> candidate.supports() == sortType)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported sort type: " + sortType));

            List<WorkItem> domains = new ArrayList<>();
            for (WorkItemEntity entity : repository.findAll()) {
                try {
                    domains.add(toDomain(entity));
                } catch (RuntimeException ignored) {
                }
            }

            List<WorkItemBoardView.ItemView> items = new ArrayList<>();
            for (WorkItem workItem : sorter.sort(domains)) {
                try {
                    items.add(toView(workItem));
                } catch (RuntimeException ignored) {
                }
            }

            WorkItemBoardView result = new WorkItemBoardView(sortType, items.size(), items, false);

            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            } catch (JsonProcessingException ignored) {
            } catch (RuntimeException ignored) {
            }

            return result;
        } catch (RuntimeException ignoredFatal) {
            return new WorkItemBoardView(sortType, 0, List.of(), false);
        }
    }

    public WorkItemBoardView.ItemView updateStatus(String workItemId, WorkItemStatus status) {
        WorkItemEntity entity = repository.findById(UUID.fromString(workItemId))
                .orElseThrow(() -> new WorkItemNotFoundException("Work item not found: " + workItemId));

        WorkItem saved = saveDomain(toDomain(entity).moveTo(status, LocalDateTime.now()));
        clearCache();
        return toView(saved);
    }

    private WorkItem saveDomain(WorkItem workItem) {
        WorkItemEntity entity = new WorkItemEntity(
                workItem.id(),
                workItem.title(),
                workItem.description(),
                workItem.status(),
                workItem.priority(),
                workItem.createdAt(),
                workItem.updatedAt()
        );

        WorkItemEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private WorkItem toDomain(WorkItemEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = entity.getCreatedAt() == null ? now : entity.getCreatedAt();
        LocalDateTime updatedAt = entity.getUpdatedAt() == null ? createdAt : entity.getUpdatedAt();
        WorkItemStatus status = entity.getStatus() == null ? WorkItemStatus.BACKLOG : entity.getStatus();
        WorkItemPriority priority = entity.getPriority() == null ? WorkItemPriority.MEDIUM : entity.getPriority();
        return new WorkItem(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                status,
                priority,
                createdAt,
                updatedAt
        );
    }

    private WorkItemBoardView.ItemView toView(WorkItem workItem) {
        if (workItem.id() == null) {
            throw new IllegalArgumentException("work item id is null");
        }
        return new WorkItemBoardView.ItemView(
                workItem.id().toString(),
                workItem.title(),
                workItem.description(),
                workItem.status(),
                workItem.priority(),
                workItem.createdAt(),
                workItem.updatedAt()
        );
    }

    private void clearCache() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ignored) {
        }
    }
}
