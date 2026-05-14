package com.sleekydz86.carebridge.backend.server.application.port.in;

public interface RegisterObservationResultUseCase {
    RegisterObservationResultResponse register(String rawMessage);

    record RegisterObservationResultResponse(
            String messageControlId,
            String status,
            String patientNo,
            String orderNo,
            int savedResultCount,
            String errorCode,
            String message,
            String ackMessage
    ) {}
}

