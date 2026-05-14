package com.sleekydz86.carebridge.backend.server.application.emr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.carebridge.backend.server.application.emr.Hl7Models.ParsedHl7Message;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EmrInterfaceService {
    private final PatientJpaRepository patientJpaRepository;
    private final ExamOrderJpaRepository examOrderJpaRepository;
    private final ObservationResultJpaRepository observationResultJpaRepository;
    private final Hl7MessageLogJpaRepository hl7MessageLogJpaRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;
    private final MedicalDeviceJpaRepository medicalDeviceJpaRepository;
    private final Hl7MessageParser hl7MessageParser;
    private final Hl7AckGenerator hl7AckGenerator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public EmrInterfaceService(
            PatientJpaRepository patientJpaRepository,
            ExamOrderJpaRepository examOrderJpaRepository,
            ObservationResultJpaRepository observationResultJpaRepository,
            Hl7MessageLogJpaRepository hl7MessageLogJpaRepository,
            AuditLogJpaRepository auditLogJpaRepository,
            MedicalDeviceJpaRepository medicalDeviceJpaRepository,
            Hl7MessageParser hl7MessageParser,
            Hl7AckGenerator hl7AckGenerator,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.patientJpaRepository = patientJpaRepository;
        this.examOrderJpaRepository = examOrderJpaRepository;
        this.observationResultJpaRepository = observationResultJpaRepository;
        this.hl7MessageLogJpaRepository = hl7MessageLogJpaRepository;
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.medicalDeviceJpaRepository = medicalDeviceJpaRepository;
        this.hl7MessageParser = hl7MessageParser;
        this.hl7AckGenerator = hl7AckGenerator;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public RegisterObservationResultResponse register(String rawMessage) {
        LocalDateTime now = LocalDateTime.now();
        ParsedHl7Message parsed = null;
        String messageControlId = "UNKNOWN";
        String errorCode = null;
        String errorMessage = null;

        try {
            parsed = hl7MessageParser.parse(rawMessage);
            messageControlId = parsed.messageControlId();

            Hl7MessageLogEntity existing = hl7MessageLogJpaRepository.findByMessageControlId(messageControlId).orElse(null);
            if (existing != null) {
                return responseFromLog(existing, "DUPLICATE_MESSAGE", "이미 처리된 HL7 메시지입니다.");
            }

            PatientEntity patient = patientJpaRepository.findByPatientNo(parsed.patientNo()).orElse(null);
            if (patient == null) {
                errorCode = "PATIENT_NOT_FOUND";
                errorMessage = "환자를 찾을 수 없습니다.";
                return fail(rawMessage, parsed, errorCode, errorMessage, now);
            }

            ExamOrderEntity order = examOrderJpaRepository.findByOrderNo(parsed.orderNo()).orElse(null);
            if (order == null || !order.getPatient().getPatientNo().equals(patient.getPatientNo())) {
                errorCode = "ORDER_NOT_FOUND";
                errorMessage = "검사오더를 찾을 수 없습니다.";
                return fail(rawMessage, parsed, errorCode, errorMessage, now);
            }

            if (order.getStatus() == ExamOrderStatus.CANCELED) {
                errorCode = "ORDER_CANCELED";
                errorMessage = "취소된 검사오더입니다.";
                return fail(rawMessage, parsed, errorCode, errorMessage, now);
            }

            ParsedHl7Message parsedMessage = parsed;
            String deviceCode = parsedMessage.deviceCode();
            List<ObservationResultEntity> results = parsed.observations().stream()
                    .map(observation -> new ObservationResultEntity(
                            null,
                            patient,
                            order,
                            parsedMessage.messageControlId(),
                            deviceCode,
                            observation.observationCode(),
                            observation.observationName(),
                            observation.resultValue(),
                            observation.unit(),
                            observation.referenceRange(),
                            observation.abnormalFlag(),
                            "FINAL",
                            now,
                            now
                    ))
                    .toList();
            observationResultJpaRepository.saveAll(results);
            order.complete(now);
            examOrderJpaRepository.save(order);
            auditLogJpaRepository.save(new AuditLogEntity(null, "OBSERVATION_RESULT_REGISTERED", "messageControlId", parsed.messageControlId(), "system", now));
            String ack = hl7AckGenerator.success(parsed.messageControlId());
            Hl7MessageLogEntity savedLog = saveLog(rawMessage, parsed, Hl7ProcessStatus.SUCCESS, null, null, "AA", ack, now);
            applicationEventPublisher.publishEvent(new Hl7RealtimeEvent(toLogView(savedLog)));
            return new RegisterObservationResultResponse(parsed.messageControlId(), "SUCCESS", parsed.patientNo(), parsed.orderNo(), results.size(), null, null, ack);
        } catch (IllegalArgumentException exception) {
            errorCode = exception.getMessage();
            errorMessage = switch (errorCode) {
                case "UNSUPPORTED_MESSAGE_TYPE" -> "지원하지 않는 HL7 메시지 타입입니다.";
                case "INVALID_HL7_FORMAT" -> "HL7 필수 세그먼트가 부족합니다.";
                default -> "HL7 메시지 파싱에 실패했습니다.";
            };
            String ack = hl7AckGenerator.failure(messageControlId, errorCode);
            Hl7MessageLogEntity savedLog = new Hl7MessageLogEntity(null, messageControlId, "UNKNOWN", "UNKNOWN", null, null, rawMessage, null, Hl7ProcessStatus.FAILED, errorCode, errorMessage, "AE", ack, now, now);
            hl7MessageLogJpaRepository.save(savedLog);
            auditLogJpaRepository.save(new AuditLogEntity(null, "HL7_MESSAGE_FAILED", "messageControlId", messageControlId, "system", now));
            applicationEventPublisher.publishEvent(new Hl7RealtimeEvent(toLogView(savedLog)));
            return new RegisterObservationResultResponse(messageControlId, "FAILED", null, null, 0, errorCode, errorMessage, ack);
        }
    }

    @Transactional(readOnly = true)
    public List<PatientView> patients() {
        return patientJpaRepository.findAll().stream().map(patient -> new PatientView(
                patient.getPatientNo(),
                patient.getName(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getCreatedAt(),
                observationResultJpaRepository.countByPatientPatientNo(patient.getPatientNo()),
                observationResultJpaRepository.findByPatientPatientNoOrderByCreatedAtDesc(patient.getPatientNo()).stream().findFirst().map(ObservationResultEntity::getCreatedAt).orElse(null)
        )).toList();
    }

    @Transactional(readOnly = true)
    public PatientDetailView patient(String patientNo) {
        PatientEntity patient = patientJpaRepository.findByPatientNo(patientNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "환자를 찾을 수 없습니다."));
        return new PatientDetailView(
                new PatientView(patient.getPatientNo(), patient.getName(), patient.getBirthDate(), patient.getGender(), patient.getCreatedAt(), observationResultJpaRepository.countByPatientPatientNo(patientNo), null),
                examOrders(patientNo),
                observationResultsByPatient(patientNo)
        );
    }

    @Transactional(readOnly = true)
    public List<ExamOrderView> examOrders() {
        return examOrderJpaRepository.findAllByOrderByOrderedAtDesc().stream().map(this::toOrderView).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamOrderView> examOrders(String patientNo) {
        return examOrderJpaRepository.findByPatientPatientNoOrderByOrderedAtDesc(patientNo).stream().map(this::toOrderView).toList();
    }

    @Transactional(readOnly = true)
    public List<ObservationResultView> observationResultsByPatient(String patientNo) {
        return observationResultJpaRepository.findByPatientPatientNoOrderByCreatedAtDesc(patientNo).stream().map(this::toResultView).toList();
    }

    @Transactional(readOnly = true)
    public List<ObservationResultView> observationResultsByOrder(String orderNo) {
        return observationResultJpaRepository.findByOrderOrderNoOrderByCreatedAtDesc(orderNo).stream().map(this::toResultView).toList();
    }

    @Transactional(readOnly = true)
    public List<Hl7MessageLogView> hl7Logs() {
        return hl7MessageLogJpaRepository.findTop50ByOrderByReceivedAtDesc().stream().map(this::toLogView).toList();
    }

    @Transactional(readOnly = true)
    public Hl7MessageLogView hl7Log(String messageControlId) {
        return hl7MessageLogJpaRepository.findByMessageControlId(messageControlId).map(this::toLogView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HL7 로그를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<MedicalDeviceView> devices() {
        return medicalDeviceJpaRepository.findAll().stream().map(device -> new MedicalDeviceView(device.getDeviceCode(), device.getDeviceName(), device.getDeviceType(), device.getIp(), device.getPort(), device.getStatus(), device.getLastConnectedAt())).toList();
    }

    private RegisterObservationResultResponse fail(String rawMessage, ParsedHl7Message parsed, String errorCode, String errorMessage, LocalDateTime now) {
        String ack = hl7AckGenerator.failure(parsed.messageControlId(), errorCode);
        Hl7MessageLogEntity savedLog = saveLog(rawMessage, parsed, Hl7ProcessStatus.FAILED, errorCode, errorMessage, "AE", ack, now);
        auditLogJpaRepository.save(new AuditLogEntity(null, "HL7_MESSAGE_FAILED", "messageControlId", parsed.messageControlId(), "system", now));
        applicationEventPublisher.publishEvent(new Hl7RealtimeEvent(toLogView(savedLog)));
        return new RegisterObservationResultResponse(parsed.messageControlId(), "FAILED", parsed.patientNo(), parsed.orderNo(), 0, errorCode, errorMessage, ack);
    }

    private Hl7MessageLogEntity saveLog(String rawMessage, ParsedHl7Message parsed, Hl7ProcessStatus status, String errorCode, String errorMessage, String ackCode, String ack, LocalDateTime now) {
        Hl7MessageLogEntity log = new Hl7MessageLogEntity(null, parsed.messageControlId(), parsed.messageType(), parsed.deviceCode(), parsed.patientNo(), parsed.orderNo(), rawMessage, toJson(parsed), status, errorCode, errorMessage, ackCode, ack, now, now);
        return hl7MessageLogJpaRepository.save(log);
    }

    private RegisterObservationResultResponse responseFromLog(Hl7MessageLogEntity log, String errorCode, String message) {
        return new RegisterObservationResultResponse(log.getMessageControlId(), log.getProcessStatus().name(), log.getPatientNo(), log.getOrderNo(), 0, errorCode, message, log.getAckMessage());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private ExamOrderView toOrderView(ExamOrderEntity order) {
        return new ExamOrderView(order.getOrderNo(), order.getPatient().getPatientNo(), order.getExamCode(), order.getExamName(), order.getStatus().name(), order.getOrderedAt(), order.getCompletedAt());
    }

    private ObservationResultView toResultView(ObservationResultEntity result) {
        return new ObservationResultView(result.getId().toString(), result.getPatient().getPatientNo(), result.getOrder().getOrderNo(), result.getMessageControlId(), result.getDeviceCode(), result.getObservationCode(), result.getObservationName(), result.getValue(), result.getUnit(), result.getReferenceRange(), result.getAbnormalFlag(), result.getResultStatus(), result.getObservedAt(), result.getCreatedAt());
    }

    private Hl7MessageLogView toLogView(Hl7MessageLogEntity log) {
        return new Hl7MessageLogView(log.getMessageControlId(), log.getMessageType(), log.getDeviceCode(), log.getPatientNo(), log.getOrderNo(), log.getRawMessage(), log.getParsedMessageJson(), log.getProcessStatus().name(), log.getErrorCode(), log.getErrorMessage(), log.getAckCode(), log.getAckMessage(), log.getReceivedAt(), log.getProcessedAt());
    }

    public record RegisterObservationResultResponse(String messageControlId, String status, String patientNo, String orderNo, int savedResultCount, String errorCode, String message, String ackMessage) {}
    public record PatientView(String patientNo, String name, String birthDate, String gender, LocalDateTime createdAt, long recentResultCount, LocalDateTime lastReceivedAt) {}
    public record PatientDetailView(PatientView patient, List<ExamOrderView> examOrders, List<ObservationResultView> observationResults) {}
    public record ExamOrderView(String orderNo, String patientNo, String examCode, String examName, String status, LocalDateTime orderedAt, LocalDateTime completedAt) {}
    public record ObservationResultView(String id, String patientNo, String orderNo, String messageControlId, String deviceCode, String observationCode, String observationName, String value, String unit, String referenceRange, String abnormalFlag, String resultStatus, LocalDateTime observedAt, LocalDateTime createdAt) {}
    public record Hl7MessageLogView(String messageControlId, String messageType, String deviceCode, String patientNo, String orderNo, String rawMessage, String parsedMessageJson, String processStatus, String errorCode, String errorMessage, String ackCode, String ackMessage, LocalDateTime receivedAt, LocalDateTime processedAt) {}
    public record MedicalDeviceView(String deviceCode, String deviceName, String deviceType, String ip, int port, String status, LocalDateTime lastConnectedAt) {}
}
