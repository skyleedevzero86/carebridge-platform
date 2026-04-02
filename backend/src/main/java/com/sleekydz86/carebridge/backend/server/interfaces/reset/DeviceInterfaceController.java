package com.sleekydz86.carebridge.backend.server.interfaces.reset;

import java.util.List;
import com.sleekydz86.carebridge.backend.server.application.device.DeviceInterfaceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-interface")
public class DeviceInterfaceController {

    private final DeviceInterfaceService deviceInterfaceService;

    public DeviceInterfaceController(DeviceInterfaceService deviceInterfaceService) {
        this.deviceInterfaceService = deviceInterfaceService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DeviceInterfaceService.DeviceOverview> overview() {
        return ResponseEntity.ok(deviceInterfaceService.overview());
    }

    @GetMapping("/events")
    public ResponseEntity<List<DeviceInterfaceService.DeviceEventView>> recentEvents() {
        return ResponseEntity.ok(deviceInterfaceService.recentEvents());
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceInterfaceService.DeviceEventView> simulate(@Valid @RequestBody SimulateDeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceInterfaceService.ingest(request.payload(), "manual-console"));
    }

    public record SimulateDeviceRequest(@NotBlank String payload)  {}
}