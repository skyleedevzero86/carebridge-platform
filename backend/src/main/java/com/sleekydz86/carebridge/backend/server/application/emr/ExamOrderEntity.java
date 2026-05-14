package com.sleekydz86.carebridge.backend.server.application.emr;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_order")
public class ExamOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 60)
    private String orderNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;
    @Column(nullable = false, length = 40)
    private String examCode;
    @Column(nullable = false, length = 120)
    private String examName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExamOrderStatus status;
    @Column(nullable = false)
    private LocalDateTime orderedAt;
    private LocalDateTime completedAt;

    protected ExamOrderEntity() {}

    public ExamOrderEntity(UUID id, String orderNo, PatientEntity patient, String examCode, String examName, ExamOrderStatus status, LocalDateTime orderedAt, LocalDateTime completedAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.patient = patient;
        this.examCode = examCode;
        this.examName = examName;
        this.status = status;
        this.orderedAt = orderedAt;
        this.completedAt = completedAt;
    }

    public void complete(LocalDateTime completedAt) {
        this.status = ExamOrderStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public UUID getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public PatientEntity getPatient() { return patient; }
    public String getExamCode() { return examCode; }
    public String getExamName() { return examName; }
    public ExamOrderStatus getStatus() { return status; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
