package com.smarthome.controller;

import com.smarthome.dto.Dto;
import com.smarthome.service.AdminOrgService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/organizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_OWNER')")
public class AdminOrgController {

    private final AdminOrgService adminOrgService;

    @GetMapping
    public List<Dto.PendingOrgDto> listOrganizations(
            @RequestParam(defaultValue = "PENDING") String status) {
        return adminOrgService.listByStatus(status);
    }

    @PostMapping("/{orgId}/review")
    public ResponseEntity<Void> reviewOrganization(
            @PathVariable String orgId,
            @Valid @RequestBody Dto.OrgApprovalRequest req) {
        adminOrgService.review(orgId, req);
        return ResponseEntity.ok().build();
    }
}
