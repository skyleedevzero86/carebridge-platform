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
@Table(name = "medical_device")
public class MedicalDeviceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 80)
    private String deviceCode;
    @Column(nullable = false, length = 120)
    private String deviceName;
    @Column(nullable = false, length = 40)
    private String deviceType;
    @Column(nullable = false, length = 80)
    private String ip;
    @Column(nullable = false)
    private int port;
    @Column(nullable = false, length = 20)
    private String status;
    private LocalDateTime lastConnectedAt;

    protected MedicalDeviceEntity() {}

    public MedicalDeviceEntity(UUID id, String deviceCode, String deviceName, String deviceType, String ip, int port, String status, LocalDateTime lastConnectedAt) {
        this.id = id;
        this.deviceCode = deviceCode;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ip = ip;
        this.port = port;
        this.status = status;
        this.lastConnectedAt = lastConnectedAt;
    }

    public UUID getId() { return id; }
    public String getDeviceCode() { return deviceCode; }
    public String getDeviceName() { return deviceName; }
    public String getDeviceType() { return deviceType; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public String getStatus() { return status; }
    public LocalDateTime getLastConnectedAt() { return lastConnectedAt; }
}
