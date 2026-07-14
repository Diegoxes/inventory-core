package com.smarthome.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.dto.Dto;
import com.smarthome.entity.InventorySnapshot;
import com.smarthome.repository.InventorySnapshotRepository;
import com.smarthome.repository.OrganizationRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.PurchaseRepository;
import com.smarthome.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventorySnapshotServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock ProductRepository productRepository;
    @Mock PurchaseRepository purchaseRepository;
    @Mock InventorySnapshotRepository snapshotRepository;
    @Mock OrganizationContextService orgContext;
    @Mock ObjectMapper objectMapper;
    @InjectMocks InventorySnapshotService inventorySnapshotService;

    @Test
    void captureDailySnapshots_savesForEachOrg() throws Exception {
        when(organizationRepository.findAll()).thenReturn(List.of(TestFixtures.organization()));
        when(productRepository.findByOrganizationId(TestFixtures.ORG_ID)).thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        inventorySnapshotService.captureDailySnapshots();

        verify(snapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    void history_returnsSnapshotsInRange() {
        when(orgContext.requireActiveOrgId()).thenReturn(TestFixtures.ORG_ID);
        InventorySnapshot snap = InventorySnapshot.builder()
                .snapshotDate(LocalDate.now())
                .totalValue(BigDecimal.TEN)
                .breakdownJson("[]")
                .build();
        when(snapshotRepository.findByOrganizationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                eq(TestFixtures.ORG_ID), any(), any())).thenReturn(List.of(snap));

        List<Dto.InventorySnapshotDto> history = inventorySnapshotService.history(null, null);

        assertEquals(1, history.size());
        assertEquals(BigDecimal.TEN, history.get(0).getTotalValue());
    }
}
