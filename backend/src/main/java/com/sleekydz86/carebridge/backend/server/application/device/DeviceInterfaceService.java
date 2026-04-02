package com.sleekydz86.carebridge.backend.server.application.device;


import java.time.LocalDateTime;
import java.util.List;

import com.sleekydz86.carebridge.backend.global.config.AppProperties;
import com.sleekydz86.carebridge.backend.global.security.InputSanitizer;
import com.sleekydz86.carebridge.backend.server.domain.device.DeviceEvent;
import com.sleekydz86.carebridge.backend.server.domain.device.DeviceEventFactory;
import com.sleekydz86.carebridge.backend.server.domain.device.DevicePayloadInterpreter;
import com.sleekydz86.carebridge.backend.server.domain.device.InterpretedDevicePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DeviceInterfaceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceInterfaceService.class);
    private static final int MAX_PAYLOAD_LENGTH = 4096;

    private final DeviceEventJpaRepository deviceEventJpaRepository;
    private final List<DevicePayloadInterpreter> devicePayloadInterpreters;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AppProperties appProperties;

    public DeviceInterfaceService(
            DeviceEventJpaRepository deviceEventJpaRepository,
            List<DevicePayloadInterpreter> devicePayloadInterpreters,
            ApplicationEventPublisher applicationEventPublisher,
            AppProperties appProperties
    ) {
        this.deviceEventJpaRepository = deviceEventJpaRepository;
        this.devicePayloadInterpreters = devicePayloadInterpreters;
        this.applicationEventPublisher = applicationEventPublisher;
        this.appProperties = appProperties;
    }

    public DeviceEventView ingest(String rawPayload, String sourceIp) {

        String normalizedPayload = InputSanitizer.sanitizePayload(rawPayload, MAX_PAYLOAD_LENGTH);
        if (normalizedPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장비 페이로드가 비어있습니다.");
        }

        InterpretedDevicePayload interpretedPayload = devicePayloadInterpreters.stream()
                .filter(interpreter -> interpreter.supports(normalizedPayload))
                .findFirst()
                .map(interpreter -> interpreter.interpret(normalizedPayload))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported device payload format."));

        DeviceEvent event = DeviceEventFactory.create(interpretedPayload, normalizedPayload, sourceIp, LocalDateTime.now());
        DeviceEventEntity saved = deviceEventJpaRepository.save(toEntity(event));
        DeviceEventView view = toView(toDomain(saved));

        log.info(
                "Device event saved. deviceCode=, protocol=, sourceIp=, ackCode=",
                view.deviceCode(),
                view.protocol(),
                view.sourceIp(),
                view.ackCode()
        );

        applicationEventPublisher.publishEvent(new DeviceRealtimeEvent(view));
        return view;
    }

    @Transactional(readOnly = true)
    public List<DeviceEventView> recentEvents() {
        return deviceEventJpaRepository.findTop25ByOrderByReceivedAtDesc().stream()
                .map(this::toDomain)
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceOverview overview() {
        return new DeviceOverview(
                appProperties.tcp().port(),
                deviceEventJpaRepository.count(),
                deviceEventJpaRepository.findFirstByOrderByReceivedAtDesc()
                        .map(DeviceEventEntity::getReceivedAt)
                        .orElse(null),
                appProperties.simulator().enabled(),
                appProperties.simulator().intervalMillis()
        );
    }

    private DeviceEventEntity toEntity(DeviceEvent event) {
        return new DeviceEventEntity(
                null,
                event.deviceCode(),
                event.protocol(),
                event.patientCode(),
                event.summary(),
                event.payload(),
                event.sourceIp(),
                event.ackCode(),
                event.receivedAt()
        );
    }

    private DeviceEvent toDomain(DeviceEventEntity entity) {
        return new DeviceEvent(
                entity.getId(),
                entity.getDeviceCode(),
                entity.getProtocol(),
                entity.getPatientCode(),
                entity.getSummary(),
                entity.getPayload(),
                entity.getSourceIp(),
                entity.getAckCode(),
                entity.getReceivedAt()
        );
    }

    private DeviceEventView toView(DeviceEvent event) {
        return new DeviceEventView(
                event.id().toString(),
                event.deviceCode(),
                event.protocol().name(),
                event.patientCode(),
                event.summary(),
                event.payload(),
                event.sourceIp(),
                event.ackCode(),
                event.receivedAt()
        );
    }

    public record DeviceEventView(
            String id,
            String deviceCode,
            String protocol,
            String patientCode,
            String summary,
            String payload,
            String sourceIp,
            String ackCode,
            LocalDateTime receivedAt
    ) {}
    public record DeviceOverview(
            int tcpPort,
            long totalMessages,
            LocalDateTime lastReceivedAt,
            boolean simulatorEnabled,
            long simulatorIntervalMillis
    ) {}
}