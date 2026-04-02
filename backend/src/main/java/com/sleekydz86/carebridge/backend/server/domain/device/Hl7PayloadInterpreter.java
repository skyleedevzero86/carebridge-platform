package com.sleekydz86.carebridge.backend.server.domain.device;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class Hl7PayloadInterpreter implements DevicePayloadInterpreter {

    @Override
    public boolean supports(String payload) {
        return payload != null && payload.contains("MSH|");
    }

    @Override
    public InterpretedDevicePayload interpret(String payload) {
        String[] segments = payload.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        String deviceCode = "HL7-GATEWAY";
        String patientCode = null;
        String summary = "HL7 메시지 수신";

        for (String segment : segments) {
            if (segment.startsWith("MSH|")) {
                String[] fields = segment.split("\\|", -1);
                if (fields.length > 2 && !fields[2].isBlank()) {
                    deviceCode = fields[2].trim();
                } else if (fields.length > 3 && !fields[3].isBlank()) {
                    deviceCode = fields[3].trim();
                }
            }

            if (segment.startsWith("PID|")) {
                String[] fields = segment.split("\\|", -1);
                if (fields.length > 3 && !fields[3].isBlank()) {
                    patientCode = fields[3].trim();
                }
            }

            if (segment.startsWith("OBX|")) {
                String[] fields = segment.split("\\|", -1);
                if (fields.length > 5 && !fields[5].isBlank()) {
                    summary = "관측값 " + fields[5].trim() + " 수신";
                }
            }
        }

        return new InterpretedDevicePayload(deviceCode, DeviceProtocol.HL7, patientCode, summary);
    }
}