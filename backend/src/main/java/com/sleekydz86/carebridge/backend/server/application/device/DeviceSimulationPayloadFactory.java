package com.sleekydz86.carebridge.backend.server.application.device;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class DeviceSimulationPayloadFactory {

    private static final DateTimeFormatter HL7_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> KEY_VALUE_DEVICES = List.of("XRAY-01", "VITAL-02", "LAB-03");
    private static final List<String> HL7_DEVICES = List.of("HL7-GATEWAY-A", "HL7-GATEWAY-B");

    private final AtomicLong sequence = new AtomicLong();

    public String nextPayload() {
        long currentSequence = sequence.incrementAndGet();
        return currentSequence % 3 == 0 ? nextHl7Payload(currentSequence) : nextKeyValuePayload(currentSequence);
    }

    private String nextKeyValuePayload(long currentSequence) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String deviceCode = KEY_VALUE_DEVICES.get(random.nextInt(KEY_VALUE_DEVICES.size()));
        String patientCode = "P-" + String.format("%04d", 1000 + (currentSequence % 9000));
        int heartRate = random.nextInt(62, 105);
        int spo2 = random.nextInt(94, 100);
        double temperature = Math.round(random.nextDouble(36.2, 37.5) * 10.0) / 10.0;

        return "DEVICE=" + deviceCode
                + "|PATIENT=" + patientCode
                + "|HEART_RATE=" + heartRate
                + "|SPO2=" + spo2
                + "|TEMP=" + temperature
                + "|STATUS=READY";
    }

    private String nextHl7Payload(long currentSequence) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String deviceCode = HL7_DEVICES.get(random.nextInt(HL7_DEVICES.size()));
        String patientCode = "P-" + String.format("%04d", 2000 + (currentSequence % 7000));
        String observationValue = String.format("%.1f", random.nextDouble(3.5, 7.8));
        String timestamp = LocalDateTime.now().format(HL7_TIME_FORMAT);

        return "MSH|^~\\&|" + deviceCode + "|CAREBRIDGE|EMR|HOSPITAL|" + timestamp + "||ORU^R01|MSG" + currentSequence + "|P|2.5\r"
                + "PID|1||" + patientCode + "||SIMULATED^PATIENT\r"
                + "OBR|1||LAB" + currentSequence + "|GLUCOSE^Glucose\r"
                + "OBX|1|NM|GLUCOSE^Glucose||" + observationValue + "|mmol/L|3.5-7.8|N\r";
    }
}