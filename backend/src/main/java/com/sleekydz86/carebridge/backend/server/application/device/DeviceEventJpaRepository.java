package com.sleekydz86.carebridge.backend.server.application.device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceEventJpaRepository extends JpaRepository<DeviceEventEntity, UUID> {
    List<DeviceEventEntity> findTop25ByOrderByReceivedAtDesc();
    Optional<DeviceEventEntity> findFirstByOrderByReceivedAtDesc();
}