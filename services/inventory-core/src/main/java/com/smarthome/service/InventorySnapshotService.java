package com.smarthome.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.dto.Dto;
import com.smarthome.entity.InventorySnapshot;
import com.smarthome.entity.Organization;
import com.smarthome.entity.Product;
import com.smarthome.entity.Purchase;
import com.smarthome.repository.InventorySnapshotRepository;
import com.smarthome.repository.OrganizationRepository;
import com.smarthome.repository.ProductRepository;
import com.smarthome.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventorySnapshotService {

    private final OrganizationRepository organizationRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final OrganizationContextService orgContext;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void captureDailySnapshots() {
        for (Organization org : organizationRepository.findAll()) {
            try {
                Dto.InventoryReportDto report = inventoryOverviewForOrg(org.getId());
                snapshotRepository.save(InventorySnapshot.builder()
                        .organization(org)
                        .snapshotDate(LocalDate.now())
                        .totalValue(report.getTotalEstimatedValue())
                        .breakdownJson(objectMapper.writeValueAsString(report.getByCategory()))
                        .build());
            } catch (Exception e) {
                log.warn("Snapshot org {}: {}", org.getId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Dto.InventorySnapshotDto> history(LocalDate from, LocalDate to) {
        String orgId = orgContext.requireActiveOrgId();
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        return snapshotRepository.findByOrganizationIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(orgId, start, end)
                .stream()
                .map(s -> Dto.InventorySnapshotDto.builder()
                        .date(s.getSnapshotDate())
                        .totalValue(s.getTotalValue())
                        .breakdownJson(s.getBreakdownJson())
                        .build())
                .collect(Collectors.toList());
    }

    private Dto.InventoryReportDto inventoryOverviewForOrg(String orgId) {
        List<Product> all = productRepository.findByOrganizationId(orgId);
        Map<String, Dto.CategoryBreakdownDto> byCat = new TreeMap<>();
        BigDecimal valuation = BigDecimal.ZERO;

        for (Product p : all) {
            String catKey = Optional.ofNullable(p.getCategory()).filter(s -> !s.isBlank()).orElse("Sin categoría");
            Dto.CategoryBreakdownDto row = byCat.computeIfAbsent(catKey, k -> Dto.CategoryBreakdownDto.builder()
                    .category(k)
                    .skuCount(0)
                    .quantitySum(0d)
                    .estimatedSpend(BigDecimal.ZERO)
                    .build());

            BigDecimal unitCost = p.getAvgCost() != null ? p.getAvgCost()
                    : purchaseRepository.findFirstByProductIdOrderByPurchasedAtDesc(p.getId())
                    .map(Purchase::getUnitPrice).orElse(null);

            BigDecimal lineVal = BigDecimal.ZERO;
            if (unitCost != null && p.getQuantity() != null) {
                lineVal = unitCost.multiply(BigDecimal.valueOf(p.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                valuation = valuation.add(lineVal);
            }

            row.setSkuCount(row.getSkuCount() + 1);
            if (p.getQuantity() != null) {
                row.setQuantitySum(row.getQuantitySum() + p.getQuantity());
            }
            row.setEstimatedSpend(row.getEstimatedSpend().add(lineVal));
        }

        return Dto.InventoryReportDto.builder()
                .totalSku(all.size())
                .totalEstimatedValue(valuation.setScale(2, RoundingMode.HALF_UP))
                .byCategory(byCat.values().stream().toList())
                .build();
    }
}
