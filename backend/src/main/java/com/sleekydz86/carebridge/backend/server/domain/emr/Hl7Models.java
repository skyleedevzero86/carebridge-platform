package com.sleekydz86.carebridge.backend.server.domain.emr;

import java.util.List;

public final class Hl7Models {
    private Hl7Models() {}

    public record ParsedHl7Message(
            String messageControlId,
            String messageType,
            String deviceCode,
            String patientNo,
            String orderNo,
            String examCode,
            String examName,
            List<ParsedObservation> observations
    ) {}

    public record ParsedObservation(
            String observationCode,
            String observationName,
            String resultValue,
            String unit,
            String referenceRange,
            String abnormalFlag
    ) {}
}

