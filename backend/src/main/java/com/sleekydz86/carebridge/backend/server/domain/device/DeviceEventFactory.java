package com.sleekydz86.carebridge.backend.server.domain.device;

import java.time.LocalDateTime;
import java.util.UUID;

public final class DeviceEventFactory {

    private DeviceEventFactory()  {}

    public static DeviceEvent create(InterpretedDevicePayload interpretedPayload, String rawPayload, String sourceIp, LocalDateTime receivedAt) {
        String normalizedPayload = rawPayload == null ? "" : rawPayload.strip();
        String normalizedSourceIp = sourceIp == null || sourceIp.isBlank() ? "unknown" : sourceIp;

        if (normalizedPayload.isBlank()) {
            throw new IllegalArgumentException("장비 수신 원문이 비어 있습니다.");
        }

        String ackCode = "ACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new DeviceEvent(
                UUID.randomUUID(),
                interpretedPayload.deviceCode(),
                interpretedPayload.protocol(),
                interpretedPayload.patientCode(),
                interpretedPayload.summary(),
                normalizedPayload,
                normalizedSourceIp,
                ackCode,
                receivedAt
        );
    }
}