package com.sleekydz86.carebridge.backend.server.application.emr;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hl7_message_log")
public class Hl7MessageLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 80)
    private String messageControlId;
    @Column(nullable = false, length = 40)
    private String messageType;
    @Column(nullable = false, length = 80)
    private String deviceCode;
    @Column(length = 40)
    private String patientNo;
    @Column(length = 60)
    private String orderNo;
    @Column(nullable = false, columnDefinition = "text")
    private String rawMessage;
    @Column(columnDefinition = "text")
    private String parsedMessageJson;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Hl7ProcessStatus processStatus;
    @Column(length = 60)
    private String errorCode;
    @Column(length = 255)
    private String errorMessage;
    @Column(nullable = false, length = 10)
    private String ackCode;
    @Column(nullable = false, columnDefinition = "text")
    private String ackMessage;
    @Column(nullable = false)
    private LocalDateTime receivedAt;
    @Column(nullable = false)
    private LocalDateTime processedAt;

    protected Hl7MessageLogEntity() {}

    public Hl7MessageLogEntity(UUID id, String messageControlId, String messageType, String deviceCode, String patientNo, String orderNo, String rawMessage, String parsedMessageJson, Hl7ProcessStatus processStatus, String errorCode, String errorMessage, String ackCode, String ackMessage, LocalDateTime receivedAt, LocalDateTime processedAt) {
        this.id = id;
        this.messageControlId = messageControlId;
        this.messageType = messageType;
        this.deviceCode = deviceCode;
        this.patientNo = patientNo;
        this.orderNo = orderNo;
        this.rawMessage = rawMessage;
        this.parsedMessageJson = parsedMessageJson;
        this.processStatus = processStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.ackCode = ackCode;
        this.ackMessage = ackMessage;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }

    public UUID getId() { return id; }
    public String getMessageControlId() { return messageControlId; }
    public String getMessageType() { return messageType; }
    public String getDeviceCode() { return deviceCode; }
    public String getPatientNo() { return patientNo; }
    public String getOrderNo() { return orderNo; }
    public String getRawMessage() { return rawMessage; }
    public String getParsedMessageJson() { return parsedMessageJson; }
    public Hl7ProcessStatus getProcessStatus() { return processStatus; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getAckCode() { return ackCode; }
    public String getAckMessage() { return ackMessage; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
