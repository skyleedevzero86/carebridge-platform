package com.sleekydz86.carebridge.backend.global.config;

import java.time.LocalDateTime;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.ExamOrderJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.MedicalDeviceEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.MedicalDeviceJpaRepository;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientEntity;
import com.sleekydz86.carebridge.backend.server.adapter.out.persistence.emr.PatientJpaRepository;
import com.sleekydz86.carebridge.backend.server.application.auth.UserEntity;
import com.sleekydz86.carebridge.backend.server.application.auth.UserJpaRepository;
import com.sleekydz86.carebridge.backend.server.domain.auth.UserRole;
import com.sleekydz86.carebridge.backend.server.domain.emr.ExamOrderStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientJpaRepository patientJpaRepository;
    private final ExamOrderJpaRepository examOrderJpaRepository;
    private final MedicalDeviceJpaRepository medicalDeviceJpaRepository;

    public DemoDataInitializer(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder, PatientJpaRepository patientJpaRepository, ExamOrderJpaRepository examOrderJpaRepository, MedicalDeviceJpaRepository medicalDeviceJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.patientJpaRepository = patientJpaRepository;
        this.examOrderJpaRepository = examOrderJpaRepository;
        this.medicalDeviceJpaRepository = medicalDeviceJpaRepository;
    }

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();
        ensureUser("admin", "Admin", "Admin1234!", UserRole.ADMIN, now);
        ensureUser("operator", "Operator", "Operator1234!", UserRole.OPERATOR, now);
        PatientEntity p0001 = ensurePatient("P0001", "HONG GILDONG", "1980-01-01", "M", now);
        PatientEntity p0002 = ensurePatient("P0002", "KIM MINA", "1992-05-12", "F", now);
        ensureExamOrder("ORD-001", p0001, "ECG", "Electrocardiogram", now);
        ensureExamOrder("ORD-002", p0002, "LAB", "Basic Chemistry", now);
        ensureDevice("ECG-001", "Fake ECG Device", "ECG", "127.0.0.1", 9093);
    }

    private void ensureUser(String username, String displayName, String password, UserRole role, LocalDateTime createdAt) {
        UserEntity entity = userJpaRepository.findByUsername(username).orElse(null);
        if (entity == null) {
            userJpaRepository.save(new UserEntity(null, username, displayName, passwordEncoder.encode(password), role, createdAt));
            return;
        }
        userJpaRepository.save(new UserEntity(entity.getId(), entity.getUsername(), displayName, passwordEncoder.encode(password), role, entity.getCreatedAt()));
    }

    private PatientEntity ensurePatient(String patientNo, String name, String birthDate, String gender, LocalDateTime createdAt) {
        return patientJpaRepository.findByPatientNo(patientNo)
                .orElseGet(() -> patientJpaRepository.save(new PatientEntity(null, patientNo, name, birthDate, gender, createdAt)));
    }

    private void ensureExamOrder(String orderNo, PatientEntity patient, String examCode, String examName, LocalDateTime orderedAt) {
        if (examOrderJpaRepository.findByOrderNo(orderNo).isEmpty()) {
            examOrderJpaRepository.save(new ExamOrderEntity(null, orderNo, patient, examCode, examName, ExamOrderStatus.ORDERED, orderedAt, null));
        }
    }

    private void ensureDevice(String deviceCode, String deviceName, String deviceType, String ip, int port) {
        if (medicalDeviceJpaRepository.findByDeviceCode(deviceCode).isEmpty()) {
            medicalDeviceJpaRepository.save(new MedicalDeviceEntity(null, deviceCode, deviceName, deviceType, ip, port, "ONLINE", null));
        }
    }
}
