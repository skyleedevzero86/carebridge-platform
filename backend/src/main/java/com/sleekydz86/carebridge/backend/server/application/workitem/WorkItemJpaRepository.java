package com.sleekydz86.carebridge.backend.server.application.workitem;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkItemJpaRepository extends JpaRepository<WorkItemEntity, UUID>
{}