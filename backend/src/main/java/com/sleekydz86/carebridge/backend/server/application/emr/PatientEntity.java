package com.sleekydz86.carebridge.backend.server.application.emr;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class PatientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 40)
    private String patientNo;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, length = 20)
    private String birthDate;
    @Column(nullable = false, length = 10)
    private String gender;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected PatientEntity() {}

    public PatientEntity(UUID id, String patientNo, String name, String birthDate, String gender, LocalDateTime createdAt) {
        this.id = id;
        this.patientNo = patientNo;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getPatientNo() { return patientNo; }
    public String getName() { return name; }
    public String getBirthDate() { return birthDate; }
    public String getGender() { return gender; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
