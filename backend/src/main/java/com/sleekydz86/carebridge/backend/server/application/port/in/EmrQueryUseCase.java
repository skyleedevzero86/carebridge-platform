package com.sleekydz86.carebridge.backend.server.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public interface EmrQueryUseCase {
    List<PatientView> patients();
    PatientDetailView patient(String patientNo);
    List<ExamOrderView> examOrders();
    List<ExamOrderView> examOrders(String patientNo);
    List<ObservationResultView> observationResultsByPatient(String patientNo);
    List<ObservationResultView> observationResultsByOrder(String orderNo);
    List<Hl7MessageLogView> hl7Logs();
    Hl7MessageLogView hl7Log(String messageControlId);
    List<MedicalDeviceView> devices();

    record PatientView(String patientNo, String name, String birthDate, String gender, LocalDateTime createdAt, long recentResultCount, LocalDateTime lastReceivedAt) {}
    record PatientDetailView(PatientView patient, List<ExamOrderView> examOrders, List<ObservationResultView> observationResults) {}
    record ExamOrderView(String orderNo, String patientNo, String examCode, String examName, String status, LocalDateTime orderedAt, LocalDateTime completedAt) {}
    record ObservationResultView(String id, String patientNo, String orderNo, String messageControlId, String deviceCode, String observationCode, String observationName, String value, String unit, String referenceRange, String abnormalFlag, String resultStatus, LocalDateTime observedAt, LocalDateTime createdAt) {}
    record Hl7MessageLogView(String messageControlId, String messageType, String deviceCode, String patientNo, String orderNo, String rawMessage, String parsedMessageJson, String processStatus, String errorCode, String errorMessage, String ackCode, String ackMessage, LocalDateTime receivedAt, LocalDateTime processedAt) {}
    record MedicalDeviceView(String deviceCode, String deviceName, String deviceType, String ip, int port, String status, LocalDateTime lastConnectedAt) {}
}

