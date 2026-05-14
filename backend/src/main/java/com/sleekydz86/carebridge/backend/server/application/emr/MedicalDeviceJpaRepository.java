package com.sleekydz86.carebridge.backend.server.application.emr;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalDeviceJpaRepository extends JpaRepository<MedicalDeviceEntity, UUID> {
    Optional<MedicalDeviceEntity> findByDeviceCode(String deviceCode);
}
