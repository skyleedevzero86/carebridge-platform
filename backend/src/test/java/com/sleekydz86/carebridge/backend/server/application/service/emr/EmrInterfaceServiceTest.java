package com.sleekydz86.carebridge.backend.server.application.service.emr;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.carebridge.backend.testsupport.OruMessageSamples;
import com.sleekydz86.carebridge.backend.server.adapter.out.hl7.DefaultHl7MessageParser;
import com.sleekydz86.carebridge.backend.server.adapter.out.hl7.Hl7AckGenerator;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.Hl7MessageLogEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.Hl7MessageLogJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.MedicalDeviceJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ObservationResultJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientJpaRepository;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase.RegisterObservationResultResponse;
import com.sleekydz86.carebridge.backend.server.application.port.out.AuditLogPort;
import com.sleekydz86.carebridge.backend.server.application.port.out.ObservationNotificationPort;
import com.sleekydz86.carebridge.backend.server.domain.emr.ExamOrderStatus;
import com.sleekydz86.carebridge.backend.server.domain.emr.Hl7ProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmrInterfaceServiceTest {

    @Mock
    private PatientJpaRepository patientJpaRepository;
    @Mock
    private ExamOrderJpaRepository examOrderJpaRepository;
    @Mock
    private ObservationResultJpaRepository observationResultJpaRepository;
    @Mock
    private Hl7MessageLogJpaRepository hl7MessageLogJpaRepository;
    @Mock
    private AuditLogPort auditLogPort;
    @Mock
    private ObservationNotificationPort notificationPort;

    private final MedicalDeviceJpaRepository medicalDeviceJpaRepository = mock(MedicalDeviceJpaRepository.class);

    private EmrInterfaceService service;

    @BeforeEach
    void setUp() {
        service = new EmrInterfaceService(
                patientJpaRepository,
                examOrderJpaRepository,
                observationResultJpaRepository,
                hl7MessageLogJpaRepository,
                medicalDeviceJpaRepository,
                new DefaultHl7MessageParser(),
                new Hl7AckGenerator(),
                auditLogPort,
                notificationPort,
                new ObjectMapper()
        );
    }

    @Test
    void duplicateMessageControlIdDoesNotSaveObservationsAgain() {
        String raw = OruMessageSamples.oruR01("DUP-1", "P0001", "ORD-001");
        Hl7MessageLogEntity prior = new Hl7MessageLogEntity(
                UUID.randomUUID(),
                "DUP-1",
                "ORU^R01",
                "ECG-001",
                "P0001",
                "ORD-001",
                raw,
                "{}",
                Hl7ProcessStatus.SUCCESS,
                null,
                null,
                "AA",
                "MSA|AA|DUP-1",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(hl7MessageLogJpaRepository.findByMessageControlId("DUP-1")).thenReturn(Optional.of(prior));

        RegisterObservationResultResponse response = service.register(raw);

        assertThat(response.errorCode()).isEqualTo("DUPLICATE_MESSAGE");
        assertThat(response.status()).isEqualTo("SUCCESS");
        verify(observationResultJpaRepository, never()).saveAll(any());
    }

    @Test
    void patientNotFoundReturnsFailedAndAeAck() {
        String raw = OruMessageSamples.oruR01("NF-1", "P9999", "ORD-001");
        when(hl7MessageLogJpaRepository.findByMessageControlId("NF-1")).thenReturn(Optional.empty());
        when(patientJpaRepository.findByPatientNo("P9999")).thenReturn(Optional.empty());

        RegisterObservationResultResponse response = service.register(raw);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("PATIENT_NOT_FOUND");
        assertThat(response.ackMessage()).contains("MSA|AE|NF-1");
        ArgumentCaptor<Hl7MessageLogEntity> logCaptor = ArgumentCaptor.forClass(Hl7MessageLogEntity.class);
        verify(hl7MessageLogJpaRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessStatus()).isEqualTo(Hl7ProcessStatus.FAILED);
        assertThat(logCaptor.getValue().getAckCode()).isEqualTo("AE");
    }

    @Test
    void orderNotFoundReturnsFailedAndAeAck() {
        String raw = OruMessageSamples.oruR01("ONF-1", "P0001", "ORD-999");
        LocalDateTime now = LocalDateTime.of(2024, 6, 1, 10, 0);
        PatientEntity patient = new PatientEntity(null, "P0001", "Test", "1980-01-01", "M", now);
        when(hl7MessageLogJpaRepository.findByMessageControlId("ONF-1")).thenReturn(Optional.empty());
        when(patientJpaRepository.findByPatientNo("P0001")).thenReturn(Optional.of(patient));
        when(examOrderJpaRepository.findByOrderNo("ORD-999")).thenReturn(Optional.empty());

        RegisterObservationResultResponse response = service.register(raw);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(response.ackMessage()).contains("MSA|AE|ONF-1");
    }

    @Test
    void successSavesObservationsAndWritesSuccessLogWithAaAck() {
        String raw = OruMessageSamples.oruR01("OK-1", "P0001", "ORD-001");
        LocalDateTime now = LocalDateTime.of(2024, 6, 1, 10, 0);
        PatientEntity patient = new PatientEntity(null, "P0001", "Test", "1980-01-01", "M", now);
        ExamOrderEntity order = new ExamOrderEntity(null, "ORD-001", patient, "ECG", "Electrocardiogram", ExamOrderStatus.ORDERED, now, null);
        when(hl7MessageLogJpaRepository.findByMessageControlId("OK-1")).thenReturn(Optional.empty());
        when(patientJpaRepository.findByPatientNo("P0001")).thenReturn(Optional.of(patient));
        when(examOrderJpaRepository.findByOrderNo("ORD-001")).thenReturn(Optional.of(order));
        when(hl7MessageLogJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterObservationResultResponse response = service.register(raw);

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.savedResultCount()).isEqualTo(1);
        assertThat(response.ackMessage()).contains("MSA|AA|OK-1");
        verify(observationResultJpaRepository).saveAll(any());
        verify(examOrderJpaRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(ExamOrderStatus.COMPLETED);
        ArgumentCaptor<Hl7MessageLogEntity> logCaptor = ArgumentCaptor.forClass(Hl7MessageLogEntity.class);
        verify(hl7MessageLogJpaRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProcessStatus()).isEqualTo(Hl7ProcessStatus.SUCCESS);
        assertThat(logCaptor.getValue().getAckCode()).isEqualTo("AA");
    }

    @Test
    void invalidHl7FormatReturnsAeAndFailedLog() {
        String raw = "MSH|^~\\&|HL7GW|APP|EMR|HOSP|20260101120000||ORU^R01|BAD|P|2.5\n"
                + "PID|1||P0001||TEST^PATIENT|||M\n";
        when(hl7MessageLogJpaRepository.findByMessageControlId("BAD")).thenReturn(Optional.empty());
        when(hl7MessageLogJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterObservationResultResponse response = service.register(raw);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("INVALID_HL7_FORMAT");
        assertThat(response.ackMessage()).contains("MSA|AE|BAD");
        verify(observationResultJpaRepository, never()).saveAll(any());
    }
}
