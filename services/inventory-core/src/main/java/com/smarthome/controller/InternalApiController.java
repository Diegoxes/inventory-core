package com.smarthome.controller;

import com.smarthome.contracts.internal.*;
import com.smarthome.dto.Dto;
import com.smarthome.service.InternalApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final InternalApiService internalApiService;

    @GetMapping("/membership/{userId}")
    public InternalMembershipResponse membership(@PathVariable String userId) {
        return internalApiService.membershipForUser(userId);
    }

    @GetMapping("/users/{userId}/permissions")
    public List<Dto.ModulePermissionDto> permissions(@PathVariable String userId) {
        return internalApiService.permissionsForUser(userId);
    }

    @PostMapping("/org-members")
    public ResponseEntity<Void> attachOrgMember(@Valid @RequestBody InternalOrgMemberRequest req) {
        internalApiService.attachOrgMember(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/products/search")
    public List<InternalProductSummary> searchProducts(
            @RequestParam String orgId,
            @RequestParam(required = false) String q) {
        return internalApiService.searchProducts(orgId, q);
    }

    @GetMapping("/products/context")
    public InternalBusinessContext businessContext(@RequestParam String orgId) {
        return internalApiService.businessContext(orgId);
    }

    @PostMapping("/inventory/consume")
    public ResponseEntity<Void> consume(@Valid @RequestBody InternalConsumeRequest req) {
        internalApiService.consume(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/inventory/restock")
    public ResponseEntity<Void> restock(@Valid @RequestBody InternalRestockRequest req) {
        internalApiService.restock(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/by-whatsapp")
    public InternalWhatsappUserResponse userByWhatsapp(@RequestParam String number) {
        return internalApiService.userByWhatsapp(number);
    }

    @PostMapping("/org/{orgId}/activate")
    public ResponseEntity<Void> activateOrg(@PathVariable String orgId) {
        internalApiService.activateOrg(orgId);
        return ResponseEntity.ok().build();
    }
}
