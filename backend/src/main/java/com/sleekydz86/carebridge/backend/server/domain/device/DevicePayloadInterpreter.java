package com.sleekydz86.carebridge.backend.server.domain.device;

public interface DevicePayloadInterpreter {
    boolean supports(String payload);
    InterpretedDevicePayload interpret(String payload);
}