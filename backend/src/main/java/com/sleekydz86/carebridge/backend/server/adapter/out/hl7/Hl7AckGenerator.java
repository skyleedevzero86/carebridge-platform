package com.sleekydz86.carebridge.backend.server.adapter.out.hl7;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class Hl7AckGenerator {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String success(String messageControlId) {
        return ack("AA", messageControlId, null);
    }

    public String failure(String messageControlId, String errorCode) {
        return ack("AE", messageControlId, errorCode);
    }

    private String ack(String code, String messageControlId, String errorCode) {
        String controlId = messageControlId == null || messageControlId.isBlank() ? "UNKNOWN" : messageControlId;
        StringBuilder builder = new StringBuilder();
        builder.append("MSH|^~\\&|EMR|HOSPITAL|ECG|DEVICE|")
                .append(LocalDateTime.now().format(FORMATTER))
                .append("||ACK|")
                .append(controlId)
                .append("|P|2.5\n");
        builder.append("MSA|").append(code).append("|").append(controlId);
        if (errorCode != null && !errorCode.isBlank()) {
            builder.append("\nERR|||").append(errorCode);
        }
        return builder.toString();
    }
}

