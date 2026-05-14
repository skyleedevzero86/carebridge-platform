package com.sleekydz86.carebridge.backend.server.interfaces.reset.emr;

import java.util.List;
import com.sleekydz86.carebridge.backend.server.application.emr.EmrInterfaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmrInterfaceController {
    private final EmrInterfaceService emrInterfaceService;

    public EmrInterfaceController(EmrInterfaceService emrInterfaceService) {
        this.emrInterfaceService = emrInterfaceService;
    }

    @PostMapping(path = "/api/interface/hl7/messages", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<EmrInterfaceService.RegisterObservationResultResponse> receiveHl7(@RequestBody String rawMessage) {
        return ResponseEntity.ok(emrInterfaceService.register(rawMessage));
    }

    @GetMapping("/api/interface/hl7/messages")
    public ResponseEntity<List<EmrInterfaceService.Hl7MessageLogView>> hl7Logs() {
        return ResponseEntity.ok(emrInterfaceService.hl7Logs());
    }

    @GetMapping("/api/interface/hl7/messages/{messageControlId}")
    public ResponseEntity<EmrInterfaceService.Hl7MessageLogView> hl7Log(@PathVariable String messageControlId) {
        return ResponseEntity.ok(emrInterfaceService.hl7Log(messageControlId));
    }

    @GetMapping("/api/patients")
    public ResponseEntity<List<EmrInterfaceService.PatientView>> patients() {
        return ResponseEntity.ok(emrInterfaceService.patients());
    }

    @GetMapping("/api/patients/{patientNo}")
    public ResponseEntity<EmrInterfaceService.PatientDetailView> patient(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrInterfaceService.patient(patientNo));
    }

    @GetMapping("/api/exam-orders")
    public ResponseEntity<List<EmrInterfaceService.ExamOrderView>> examOrders() {
        return ResponseEntity.ok(emrInterfaceService.examOrders());
    }

    @GetMapping("/api/patients/{patientNo}/exam-orders")
    public ResponseEntity<List<EmrInterfaceService.ExamOrderView>> patientExamOrders(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrInterfaceService.examOrders(patientNo));
    }

    @GetMapping("/api/patients/{patientNo}/observation-results")
    public ResponseEntity<List<EmrInterfaceService.ObservationResultView>> patientObservationResults(@PathVariable String patientNo) {
        return ResponseEntity.ok(emrInterfaceService.observationResultsByPatient(patientNo));
    }

    @GetMapping("/api/exam-orders/{orderNo}/observation-results")
    public ResponseEntity<List<EmrInterfaceService.ObservationResultView>> orderObservationResults(@PathVariable String orderNo) {
        return ResponseEntity.ok(emrInterfaceService.observationResultsByOrder(orderNo));
    }

    @GetMapping("/api/devices")
    public ResponseEntity<List<EmrInterfaceService.MedicalDeviceView>> devices() {
        return ResponseEntity.ok(emrInterfaceService.devices());
    }

    @PostMapping("/api/device-interface/simulate/hl7")
    public ResponseEntity<EmrInterfaceService.RegisterObservationResultResponse> simulateHl7(@Valid @RequestBody SimulateHl7Request request) {
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
        return ResponseEntity.ok(emrInterfaceService.register(rawMessage));
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
