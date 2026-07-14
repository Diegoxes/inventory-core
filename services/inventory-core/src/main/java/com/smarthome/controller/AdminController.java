package com.smarthome.controller;

import com.smarthome.config.MaintenanceState;
import com.smarthome.dto.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_OWNER')")
public class AdminController {

    private final MaintenanceState maintenanceState;

    @GetMapping("/maintenance")
    public Map<String, Boolean> getMaintenance() {
        return Map.of("enabled", maintenanceState.isEnabled());
    }

    @PutMapping("/maintenance")
    public ResponseEntity<Void> setMaintenance(@Valid @RequestBody Dto.MaintenanceToggleRequest body) {
        maintenanceState.setEnabled(body.isEnabled());
        return ResponseEntity.ok().build();
    }
}
