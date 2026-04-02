package com.sleekydz86.carebridge.backend.server.domain.device;

public record InterpretedDevicePayload(
        String deviceCode,
        DeviceProtocol protocol,
        String patientCode,
        String summary
) {}