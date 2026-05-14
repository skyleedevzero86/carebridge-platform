package com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "observation_result")
public class ObservationResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private ExamOrderEntity order;
    @Column(nullable = false, length = 80)
    private String messageControlId;
    @Column(nullable = false, length = 80)
    private String deviceCode;
    @Column(nullable = false, length = 40)
    private String observationCode;
    @Column(nullable = false, length = 120)
    private String observationName;
    @Column(nullable = false, length = 120)
    private String value;
    @Column(length = 40)
    private String unit;
    @Column(length = 80)
    private String referenceRange;
    @Column(length = 20)
    private String abnormalFlag;
    @Column(nullable = false, length = 20)
    private String resultStatus;
    @Column(nullable = false)
    private LocalDateTime observedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ObservationResultEntity() {}

    public ObservationResultEntity(UUID id, PatientEntity patient, ExamOrderEntity order, String messageControlId, String deviceCode, String observationCode, String observationName, String value, String unit, String referenceRange, String abnormalFlag, String resultStatus, LocalDateTime observedAt, LocalDateTime createdAt) {
        this.id = id;
        this.patient = patient;
        this.order = order;
        this.messageControlId = messageControlId;
        this.deviceCode = deviceCode;
        this.observationCode = observationCode;
        this.observationName = observationName;
        this.value = value;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.abnormalFlag = abnormalFlag;
        this.resultStatus = resultStatus;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public PatientEntity getPatient() { return patient; }
    public ExamOrderEntity getOrder() { return order; }
    public String getMessageControlId() { return messageControlId; }
    public String getDeviceCode() { return deviceCode; }
    public String getObservationCode() { return observationCode; }
    public String getObservationName() { return observationName; }
    public String getValue() { return value; }
    public String getUnit() { return unit; }
    public String getReferenceRange() { return referenceRange; }
    public String getAbnormalFlag() { return abnormalFlag; }
    public String getResultStatus() { return resultStatus; }
    public LocalDateTime getObservedAt() { return observedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

