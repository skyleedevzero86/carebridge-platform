package com.sleekydz86.carebridge.backend.server.application.service.emr;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.carebridge.backend.server.adapter.out.hl7.Hl7AckGenerator;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.Hl7MessageLogEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.Hl7MessageLogJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.MedicalDeviceJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ObservationResultEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ObservationResultJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientJpaRepository;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.ExamOrderView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.Hl7MessageLogView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.MedicalDeviceView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.ObservationResultView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.PatientDetailView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.PatientView;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase.RegisterObservationResultResponse;
import com.sleekydz86.carebridge.backend.server.application.port.out.AuditLogPort;
import com.sleekydz86.carebridge.backend.server.application.port.out.Hl7MessageParser;
import com.sleekydz86.carebridge.backend.server.application.port.out.ObservationNotificationPort;
import com.sleekydz86.carebridge.backend.server.domain.emr.ExamOrderStatus;
import com.sleekydz86.carebridge.backend.server.domain.emr.Hl7Models.ParsedHl7Message;
import com.sleekydz86.carebridge.backend.server.domain.emr.Hl7ProcessStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class EmrInterfaceService implements RegisterObservationResultUseCase, EmrQueryUseCase {
    private final PatientJpaRepository patientJpaRepository;
    private final ExamOrderJpaRepository examOrderJpaRepository;
    private final ObservationResultJpaRepository observationResultJpaRepository;
    private final Hl7MessageLogJpaRepository hl7MessageLogJpaRepository;
    private final MedicalDeviceJpaRepository medicalDeviceJpaRepository;
    private final Hl7MessageParser hl7MessageParser;
    private final Hl7AckGenerator hl7AckGenerator;
    private final AuditLogPort auditLogPort;
    private final ObservationNotificationPort notificationPort;
    private final ObjectMapper objectMapper;

    public EmrInterfaceService(
            PatientJpaRepository patientJpaRepository,
            ExamOrderJpaRepository examOrderJpaRepository,
            ObservationResultJpaRepository observationResultJpaRepository,
            Hl7MessageLogJpaRepository hl7MessageLogJpaRepository,
            MedicalDeviceJpaRepository medicalDeviceJpaRepository,
            Hl7MessageParser hl7MessageParser,
            Hl7AckGenerator hl7AckGenerator,
            AuditLogPort auditLogPort,
            ObservationNotificationPort notificationPort,
            ObjectMapper objectMapper
    ) {
        this.patientJpaRepository = patientJpaRepository;
        this.examOrderJpaRepository = examOrderJpaRepository;
        this.observationResultJpaRepository = observationResultJpaRepository;
        this.hl7MessageLogJpaRepository = hl7MessageLogJpaRepository;
        this.medicalDeviceJpaRepository = medicalDeviceJpaRepository;
        this.hl7MessageParser = hl7MessageParser;
        this.hl7AckGenerator = hl7AckGenerator;
        this.auditLogPort = auditLogPort;
        this.notificationPort = notificationPort;
        this.objectMapper = objectMapper;
    }

    @Override
    public RegisterObservationResultResponse register(String rawMessage) {
        LocalDateTime now = LocalDateTime.now();
        ParsedHl7Message parsed = null;
        String messageControlId = "UNKNOWN";
        String errorCode;
        String errorMessage;

        try {
            parsed = hl7MessageParser.parse(rawMessage);
            messageControlId = parsed.messageControlId();

            Hl7MessageLogEntity existing = hl7MessageLogJpaRepository.findByMessageControlId(messageControlId).orElse(null);
            if (existing != null) {
                return responseFromLog(existing, "DUPLICATE_MESSAGE", "Already processed HL7 message.");
            }

            PatientEntity patient = patientJpaRepository.findByPatientNo(parsed.patientNo()).orElse(null);
            if (patient == null) {
                return fail(rawMessage, parsed, "PATIENT_NOT_FOUND", "Patient not found.", now);
            }

            ExamOrderEntity order = examOrderJpaRepository.findByOrderNo(parsed.orderNo()).orElse(null);
            if (order == null || !order.getPatient().getPatientNo().equals(patient.getPatientNo())) {
                return fail(rawMessage, parsed, "ORDER_NOT_FOUND", "Exam order not found.", now);
            }

            if (order.getStatus() == ExamOrderStatus.CANCELED) {
                return fail(rawMessage, parsed, "ORDER_CANCELED", "Exam order is canceled.", now);
            }

            ParsedHl7Message parsedMessage = parsed;
            List<ObservationResultEntity> results = parsedMessage.observations().stream()
                    .map(observation -> new ObservationResultEntity(
                            null,
                            patient,
                            order,
                            parsedMessage.messageControlId(),
                            parsedMessage.deviceCode(),
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
            auditLogPort.save("OBSERVATION_RESULT_REGISTERED", "messageControlId", parsed.messageControlId(), "system");
            String ack = hl7AckGenerator.success(parsed.messageControlId());
            Hl7MessageLogEntity savedLog = saveLog(rawMessage, parsed, Hl7ProcessStatus.SUCCESS, null, null, "AA", ack, now);
            notificationPort.notifyHl7Processed(toLogView(savedLog));
            return new RegisterObservationResultResponse(parsed.messageControlId(), "SUCCESS", parsed.patientNo(), parsed.orderNo(), results.size(), null, null, ack);
        } catch (IllegalArgumentException exception) {
            errorCode = exception.getMessage();
            errorMessage = switch (errorCode) {
                case "UNSUPPORTED_MESSAGE_TYPE" -> "Unsupported HL7 message type.";
                case "INVALID_HL7_FORMAT" -> "Required HL7 segments are missing.";
                default -> "HL7 parser failed.";
            };
            String ack = hl7AckGenerator.failure(messageControlId, errorCode);
            Hl7MessageLogEntity savedLog = new Hl7MessageLogEntity(null, messageControlId, "UNKNOWN", "UNKNOWN", null, null, rawMessage, null, Hl7ProcessStatus.FAILED, errorCode, errorMessage, "AE", ack, now, now);
            hl7MessageLogJpaRepository.save(savedLog);
            auditLogPort.save("HL7_MESSAGE_FAILED", "messageControlId", messageControlId, "system");
            notificationPort.notifyHl7Processed(toLogView(savedLog));
            return new RegisterObservationResultResponse(messageControlId, "FAILED", null, null, 0, errorCode, errorMessage, ack);
        }
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public PatientDetailView patient(String patientNo) {
        PatientEntity patient = patientJpaRepository.findByPatientNo(patientNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found."));
        return new PatientDetailView(
                new PatientView(patient.getPatientNo(), patient.getName(), patient.getBirthDate(), patient.getGender(), patient.getCreatedAt(), observationResultJpaRepository.countByPatientPatientNo(patientNo), null),
                examOrders(patientNo),
                observationResultsByPatient(patientNo)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamOrderView> examOrders() {
        return examOrderJpaRepository.findAllByOrderByOrderedAtDesc().stream().map(this::toOrderView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamOrderView> examOrders(String patientNo) {
        return examOrderJpaRepository.findByPatientPatientNoOrderByOrderedAtDesc(patientNo).stream().map(this::toOrderView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationResultView> observationResultsByPatient(String patientNo) {
        return observationResultJpaRepository.findByPatientPatientNoOrderByCreatedAtDesc(patientNo).stream().map(this::toResultView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationResultView> observationResultsByOrder(String orderNo) {
        return observationResultJpaRepository.findByOrderOrderNoOrderByCreatedAtDesc(orderNo).stream().map(this::toResultView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hl7MessageLogView> hl7Logs() {
        return hl7MessageLogJpaRepository.findTop50ByOrderByReceivedAtDesc().stream().map(this::toLogView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Hl7MessageLogView hl7Log(String messageControlId) {
        return hl7MessageLogJpaRepository.findByMessageControlId(messageControlId).map(this::toLogView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HL7 log not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalDeviceView> devices() {
        return medicalDeviceJpaRepository.findAll().stream()
                .map(device -> new MedicalDeviceView(device.getDeviceCode(), device.getDeviceName(), device.getDeviceType(), device.getIp(), device.getPort(), device.getStatus(), device.getLastConnectedAt()))
                .toList();
    }

    private RegisterObservationResultResponse fail(String rawMessage, ParsedHl7Message parsed, String errorCode, String errorMessage, LocalDateTime now) {
        String ack = hl7AckGenerator.failure(parsed.messageControlId(), errorCode);
        Hl7MessageLogEntity savedLog = saveLog(rawMessage, parsed, Hl7ProcessStatus.FAILED, errorCode, errorMessage, "AE", ack, now);
        auditLogPort.save("HL7_MESSAGE_FAILED", "messageControlId", parsed.messageControlId(), "system");
        notificationPort.notifyHl7Processed(toLogView(savedLog));
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
}
