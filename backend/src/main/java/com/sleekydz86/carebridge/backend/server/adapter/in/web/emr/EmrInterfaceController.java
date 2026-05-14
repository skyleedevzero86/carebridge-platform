package com.sleekydz86.carebridge.backend.server.adapter.in.web.emr;

import java.util.List;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.ExamOrderView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.Hl7MessageLogView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.MedicalDeviceView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.ObservationResultView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.PatientDetailView;
import com.sleekydz86.carebridge.backend.server.application.port.in.EmrQueryUseCase.PatientView;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase;
import com.sleekydz86.carebridge.backend.server.application.port.in.RegisterObservationResultUseCase.RegisterObservationResultResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmrInterfaceController {
    private final RegisterObservationResultUseCase registerObservationResultUseCase;
    private final EmrQueryUseCase emrQueryUseCase;

    public EmrInterfaceController(RegisterObservationResultUseCase registerObservationResultUseCase, EmrQueryUseCase emrQueryUseCase) {
        this.registerObservationResultUseCase = registerObservationResultUseCase;
        this.emrQueryUseCase = emrQueryUseCase;
    }

    @PostMapping(path = "/api/interface/hl7/messages", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<RegisterObservationResultResponse> receiveHl7(@RequestBody String rawMessage) {
        return ResponseEntity.ok(registerObservationResultUseCase.register(rawMessage));
    }

    @GetMapping("/api/interface/hl7/messages")
    public ResponseEntity<List<Hl7MessageLogView>> hl7Logs() {
        return ResponseEntity.ok(emrQueryUseCase.hl7Logs());
    }

    @GetMapping("/api/interface/hl7/messages/{messageControlId}")
    public ResponseEntity<Hl7MessageLogView> hl7Log(@PathVariable String messageControlId) {
        return ResponseEntity.ok(emrQueryUseCase.hl7Log(messageControlId));
    }

    @GetMapping("/api/patients")
    public ResponseEntity<List<PatientView>> patients() {
        return ResponseEntity.ok(emrQueryUseCase.patients());
    }

    @GetMapping("/api/patients/{patientNo}")
    public ResponseEntity<PatientDetailView> patient(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrQueryUseCase.patient(patientNo));
    }

    @GetMapping("/api/exam-orders")
    public ResponseEntity<List<ExamOrderView>> examOrders() {
        return ResponseEntity.ok(emrQueryUseCase.examOrders());
    }

    @GetMapping("/api/patients/{patientNo}/exam-orders")
    public ResponseEntity<List<ExamOrderView>> patientExamOrders(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrQueryUseCase.examOrders(patientNo));
    }

    @GetMapping("/api/patients/{patientNo}/observation-results")
    public ResponseEntity<List<ObservationResultView>> patientObservationResults(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrQueryUseCase.observationResultsByPatient(patientNo));
    }

    @GetMapping("/api/exam-orders/{orderNo}/observation-results")
    public ResponseEntity<List<ObservationResultView>> orderObservationResults(@PathVariable String orderNo) {
        return ResponseEntity.ok(emrQueryUseCase.observationResultsByOrder(orderNo));
    }

    @GetMapping("/api/devices")
    public ResponseEntity<List<MedicalDeviceView>> devices() {
        return ResponseEntity.ok(emrQueryUseCase.devices());
    }

    @PostMapping("/api/device-interface/simulate/hl7")
    public ResponseEntity<RegisterObservationResultResponse> simulateHl7(@Valid @RequestBody SimulateHl7Request request) {
        String rawMessage = """
                MSH|^~\\&|%s|DEVICE|EMR|HOSPITAL|20260514103000||ORU^R01|%s|P|2.5
                PID|||%s||PATIENT^NAME||19800101|M
                OBR|1|%s||%s^%s
                OBX|1|NM|%s^%s||%s|%s|%s|%s|||F
                """.formatted(
                request.deviceCode(),
                request.messageControlId(),
                request.patientNo(),
                request.orderNo(),
                request.examCode(),
                request.examName(),
                request.observationCode(),
                request.observationName(),
                request.value(),
                request.unit(),
                request.referenceRange(),
                request.abnormalFlag()
        );
        return ResponseEntity.ok(registerObservationResultUseCase.register(rawMessage));
    }

    public record SimulateHl7Request(
            @NotBlank String messageControlId,
            @NotBlank String deviceCode,
            @NotBlank String patientNo,
            @NotBlank String orderNo,
            @NotBlank String examCode,
            @NotBlank String examName,
            @NotBlank String observationCode,
            @NotBlank String observationName,
            @NotBlank String value,
            String unit,
            String referenceRange,
            String abnormalFlag
    ) {}
}
