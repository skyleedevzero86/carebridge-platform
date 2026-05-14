package com.sleekydz86.carebridge.backend.server.application.emr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.sleekydz86.carebridge.backend.server.application.emr.Hl7Models.ParsedHl7Message;
import com.sleekydz86.carebridge.backend.server.application.emr.Hl7Models.ParsedObservation;
import org.springframework.stereotype.Component;

@Component
public class Hl7MessageParser {
    public ParsedHl7Message parse(String rawMessage) {
        String[] segments = rawMessage.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        String[] msh = null;
        String[] pid = null;
        String[] obr = null;
        List<String[]> obxSegments = new ArrayList<>();

        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String[] fields = trimmed.split("\\|", -1);
            switch (fields[0]) {
                case "MSH" -> msh = fields;
                case "PID" -> pid = fields;
                case "OBR" -> obr = fields;
                case "OBX" -> obxSegments.add(fields);
                default -> {
                }
            }
        }

        if (msh == null || pid == null || obr == null || obxSegments.isEmpty()) {
            throw new IllegalArgumentException("INVALID_HL7_FORMAT");
        }

        String messageType = field(msh, 8);
        if (!"ORU^R01".equals(messageType)) {
            throw new IllegalArgumentException("UNSUPPORTED_MESSAGE_TYPE");
        }

        List<ParsedObservation> observations = obxSegments.stream()
                .map(obx -> {
                    String[] code = component(field(obx, 3));
                    return new ParsedObservation(
                            value(code, 0),
                            value(code, 1),
                            field(obx, 5),
                            field(obx, 6),
                            field(obx, 7),
                            field(obx, 8)
                    );
                })
                .toList();

        String[] exam = component(field(obr, 4));
        return new ParsedHl7Message(
                field(msh, 9),
                messageType,
                blankToDefault(field(msh, 2), "ECG-001"),
                field(pid, 3),
                field(obr, 2),
                value(exam, 0),
                value(exam, 1),
                observations
        );
    }

    private String field(String[] fields, int index) {
        if (index >= fields.length) {
            return "";
        }
        return fields[index].trim();
    }

    private String[] component(String value) {
        return Arrays.stream(value.split("\\^", -1)).map(String::trim).toArray(String[]::new);
    }

    private String value(String[] values, int index) {
        if (index >= values.length || values[index].isBlank()) {
            return "";
        }
        return values[index];
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
