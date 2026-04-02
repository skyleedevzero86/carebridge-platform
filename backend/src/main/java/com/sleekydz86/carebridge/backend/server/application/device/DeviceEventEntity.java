package com.sleekydz86.carebridge.backend.server.application.device;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sleekydz86.carebridge.backend.server.domain.device.DeviceProtocol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "device_event")
public class DeviceEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String deviceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceProtocol protocol;

    @Column(length = 80)
    private String patientCode;

    @Column(nullable = false, length = 255)
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, length = 80)
    private String sourceIp;

    @Column(nullable = false, length = 40)
    private String ackCode;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    protected DeviceEventEntity()  {}

    public DeviceEventEntity(
            UUID id,
            String deviceCode,
            DeviceProtocol protocol,
            String patientCode,
            String summary,
            String payload,
            String sourceIp,
            String ackCode,
            LocalDateTime receivedAt
    ) {
        this.id = id;
        this.deviceCode = deviceCode;
        this.protocol = protocol;
        this.patientCode = patientCode;
        this.summary = summary;
        this.payload = payload;
        this.sourceIp = sourceIp;
        this.ackCode = ackCode;
        this.receivedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public DeviceProtocol getProtocol() {
        return protocol;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayload() {
        return payload;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getAckCode() {
        return ackCode;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}