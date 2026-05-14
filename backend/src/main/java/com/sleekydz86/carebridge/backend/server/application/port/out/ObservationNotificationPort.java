package com.sleekydz86.carebridge.backend.server.application.port.out;

import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.Hl7MessageLogView;

public interface ObservationNotificationPort {
    void notifyHl7Processed(Hl7MessageLogView log);
}

