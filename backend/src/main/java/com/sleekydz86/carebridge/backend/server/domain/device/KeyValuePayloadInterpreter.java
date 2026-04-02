package com.sleekydz86.carebridge.backend.server.domain.device;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class KeyValuePayloadInterpreter implements DevicePayloadInterpreter {

    @Override
    public boolean supports(String payload) {
        return payload != null && !payload.isBlank();
    }

    @Override
    public InterpretedDevicePayload interpret(String payload) {
        Map<String, String> values = new LinkedHashMap<>();

        for (String part : payload.split("\\|")) {
            String[] token = part.split("=", 2);
            if (token.length == 2) {
                values.put(token[0].trim().toUpperCase(), token[1].trim());
            }
        }

        String deviceCode = values.getOrDefault("DEVICE", "TCP-SENSOR");
        String patientCode = values.get("PATIENT");

        String summary = values.entrySet().stream()
                .filter(entry -> !"DEVICE".equals(entry.getKey()) && !"PATIENT".equals(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));

        if (summary.isBlank()) {
            summary = "원시 센서 메시지 수신";
        }

        return new InterpretedDevicePayload(deviceCode, DeviceProtocol.KEY_VALUE, patientCode, summary);
    }
}