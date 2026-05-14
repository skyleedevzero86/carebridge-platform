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
        String deviceCode = "ECG-001";
        boolean patientA = (currentSequence & 1) == 0;
        String patientNo = patientA ? "P0001" : "P0002";
        String orderNo = patientA ? "ORD-001" : "ORD-002";
        String examCode = patientA ? "ECG" : "LAB";
        String examName = patientA ? "심전도" : "기본 혈액화학";
        String observationValue = patientA
                ? String.valueOf(random.nextInt(62, 105))
                : String.format("%.1f", random.nextDouble(3.5, 7.8));
        String obxCode = patientA ? "HR" : "GLUCOSE";
        String obxName = patientA ? "Heart Rate" : "Glucose";
        String unit = patientA ? "bpm" : "mmol/L";
        String referenceRange = patientA ? "60-100" : "3.5-7.8";
        String timestamp = LocalDateTime.now().format(HL7_TIME_FORMAT);

        return "MSH|^~\\&|" + deviceCode + "|CAREBRIDGE|EMR|HOSPITAL|" + timestamp + "||ORU^R01|MSG" + currentSequence + "|P|2.5\r"
                + "PID|1||" + patientNo + "||SIMULATED^PATIENT\r"
                + "OBR|1|" + orderNo + "||" + examCode + "^" + examName + "\r"
                + "OBX|1|NM|" + obxCode + "^" + obxName + "||" + observationValue + "|" + unit + "|" + referenceRange + "|N\r";
    }
}