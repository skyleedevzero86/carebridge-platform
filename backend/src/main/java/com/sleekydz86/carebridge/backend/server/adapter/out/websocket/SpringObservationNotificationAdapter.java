package com.sleekydz86.carebridge.backend.server.adapter.out.websocket;

import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.Hl7MessageLogView;
import com.sleekydz86.carebridge.backend.server.application.port.out.ObservationNotificationPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringObservationNotificationAdapter implements ObservationNotificationPort {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringObservationNotificationAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void notifyHl7Processed(Hl7MessageLogView log) {
        applicationEventPublisher.publishEvent(new Hl7RealtimeEvent(log));
    }
}
