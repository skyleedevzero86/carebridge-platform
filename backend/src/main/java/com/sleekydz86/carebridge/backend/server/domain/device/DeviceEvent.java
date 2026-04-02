package com.sleekydz86.carebridge.backend.server.domain.device;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceEvent(
        UUID id,
        String deviceCode,
        DeviceProtocol protocol,
        String patientCode,
        String summary,
        String payload,
        String sourceIp,
        String ackCode,
        LocalDateTime receivedAt
) {}