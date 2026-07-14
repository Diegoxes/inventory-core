package com.smarthome.service;

import com.smarthome.contracts.internal.*;
import com.smarthome.dto.Dto;
import com.smarthome.entity.*;
import com.smarthome.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalApiService {

    private static final int MAX_CATALOG_PREVIEW_LINES = 40;

    private final OrganizationMemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final UserPermissionService userPermissionService;
    private final OrgMemberAttachService orgMemberAttachService;
    private final OrganizationService organizationService;
    private final ProductRepository productRepository;
    private final MeasureUnitRepository measureUnitRepository;
    private final ProductUomService productUomService;
    private final InventoryLotService inventoryLotService;
    private final ConsumptionLogRepository consumptionLogRepository;
    private final PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public InternalMembershipResponse membershipForUser(String userId) {
        OrganizationMember member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Usuario sin membresía de organización"));
        Organization org = member.getOrganization();
        return InternalMembershipResponse.builder()
                .orgId(org.getId())
                .orgRole(member.getOrgRole().name())
                .orgStatus(org.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Dto.ModulePermissionDto> permissionsForUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        return userPermissionService.modulePermissionsForUserId(userId);
    }

    @Transactional
    public void attachOrgMember(InternalOrgMemberRequest req) {
        OrganizationMember.OrgRole orgRole = OrganizationMember.OrgRole.valueOf(req.getOrgRole());
        orgMemberAttachService.attachToOrganization(req.getUserId(), req.getOrganizationId(), orgRole);
    }

    @Transactional(readOnly = true)
    public List<InternalProductSummary> searchProducts(String orgId, String q) {
        ensureOrgExists(orgId);
        String query = q != null ? q.trim() : "";
        List<Product> products = query.isBlank()
                ? productRepository.findByOrganizationIdOrderByName(orgId)
                : productRepository.searchByOrganizationId(orgId, query);
        return products.stream().map(this::toProductSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InternalBusinessContext businessContext(String orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organización no encontrada"));
        List<Product> all = productRepository.findByOrganizationIdOrderByName(orgId);
        List<Product> lowStock = productRepository.findLowStockByOrganizationId(orgId);

        BigDecimal valuation = BigDecimal.ZERO;
        for (Product p : all) {
            BigDecimal unitCost = p.getAvgCost() != null ? p.getAvgCost()
                    : purchaseRepository.findFirstByProductIdOrderByPurchasedAtDesc(p.getId())
                    .map(Purchase::getUnitPrice).orElse(null);
            if (unitCost != null && p.getQuantity() != null) {
                valuation = valuation.add(
                        unitCost.multiply(BigDecimal.valueOf(p.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            }
        }

        String catalogPreview = all.stream()
                .limit(MAX_CATALOG_PREVIEW_LINES)
                .map(p -> p.getName() + " | " + p.getQuantity() + " " + p.getUnit().name() + " | " + p.getSku())
                .collect(Collectors.joining("\n"));
        if (all.size() > MAX_CATALOG_PREVIEW_LINES) {
            catalogPreview += "\n... y " + (all.size() - MAX_CATALOG_PREVIEW_LINES) + " productos más.";
        }

        return InternalBusinessContext.builder()
                .orgName(org.getName())
                .productCount(all.size())
                .lowStockCount(lowStock.size())
                .totalInventoryValue(valuation.doubleValue())
                .catalogPreview(catalogPreview)
                .build();
    }

    @Transactional
    public void consume(InternalConsumeRequest req) {
        Product product = findProduct(req.getOrgId(), req.getProductId());
        if (!userRepository.existsById(req.getUserId())) {
            throw new RuntimeException("Usuario no encontrado");
        }
        double stockUnits = resolveBaseUnits(product, req.getOrgId(), req.getQuantity(), req.getMeasureUnitCode());
        inventoryLotService.consumeFifo(product, stockUnits);
        product.setQuantity(Math.max(0, product.getQuantity() - stockUnits));
        productRepository.save(product);
        consumptionLogRepository.save(ConsumptionLog.builder()
                .product(product)
                .quantityChange(-stockUnits)
                .actionType(ConsumptionLog.ActionType.CONSUMED)
                .source(resolveSource(req.getSource()))
                .inputQuantity(req.getQuantity())
                .build());
    }

    @Transactional
    public void restock(InternalRestockRequest req) {
        if (req.getProductId() == null || req.getProductId().isBlank()) {
            restockNewProduct(req);
            return;
        }
        Product product = findProduct(req.getOrgId(), req.getProductId());
        userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        double stockUnits = resolveBaseUnits(product, req.getOrgId(), req.getQuantity(), req.getMeasureUnitCode());
        product.setQuantity(product.getQuantity() + stockUnits);
        productRepository.save(product);
        inventoryLotService.addLot(product, stockUnits, product.getExpiryDate());
        consumptionLogRepository.save(ConsumptionLog.builder()
                .product(product)
                .quantityChange(stockUnits)
                .actionType(ConsumptionLog.ActionType.RESTOCKED)
                .source(resolveSource(req.getSource()))
                .inputQuantity(req.getQuantity())
                .build());
    }

    private void restockNewProduct(InternalRestockRequest req) {
        OrganizationMember member = memberRepository.findByUserId(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario sin organización"));
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Product.UnitType unit = Product.UnitType.UNIT;
        if (req.getUnitType() != null && !req.getUnitType().isBlank()) {
            try {
                unit = Product.UnitType.valueOf(req.getUnitType().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                unit = Product.UnitType.UNIT;
            }
        }
        String name = req.getProductName() != null && !req.getProductName().isBlank()
                ? req.getProductName().trim()
                : "Ítem";
        Product product = Product.builder()
                .organization(member.getOrganization())
                .user(user)
                .sku("WA-" + System.currentTimeMillis())
                .name(name)
                .quantity(req.getQuantity())
                .minQuantity(1.0)
                .unit(unit)
                .consumptionPerUse(1.0)
                .build();
        productRepository.save(product);
        inventoryLotService.addLot(product, req.getQuantity(), null);
        consumptionLogRepository.save(ConsumptionLog.builder()
                .product(product)
                .quantityChange(req.getQuantity())
                .actionType(ConsumptionLog.ActionType.RESTOCKED)
                .source(resolveSource(req.getSource()))
                .inputQuantity(req.getQuantity())
                .build());
    }

    @Transactional(readOnly = true)
    public InternalWhatsappUserResponse userByWhatsapp(String number) {
        String normalized = number != null ? number.trim() : "";
        if (normalized.isBlank()) {
            throw new RuntimeException("Número de WhatsApp requerido");
        }
        OrganizationMember member = memberRepository.findByUserWhatsappNumber(normalized)
                .or(() -> userRepository.findByWhatsappNumber(normalized)
                        .flatMap(u -> memberRepository.findByUserId(u.getId())))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para WhatsApp: " + normalized));
        return InternalWhatsappUserResponse.builder()
                .userId(member.getUser().getId())
                .orgId(member.getOrganization().getId())
                .build();
    }

    @Transactional
    public void activateOrg(String orgId) {
        organizationService.activateOrg(orgId);
    }

    private Product findProduct(String orgId, String productId) {
        return productRepository.findByIdAndOrganizationId(productId, orgId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    private void ensureOrgExists(String orgId) {
        if (!organizationRepository.existsById(orgId)) {
            throw new RuntimeException("Organización no encontrada");
        }
    }

    private InternalProductSummary toProductSummary(Product p) {
        return InternalProductSummary.builder()
                .id(p.getId())
                .name(p.getName())
                .category(p.getCategory())
                .currentStock(p.getQuantity() != null ? p.getQuantity() : 0d)
                .unitType(p.getUnit() != null ? p.getUnit().name() : null)
                .minStock(p.getMinQuantity())
                .expiryDate(p.getExpiryDate() != null ? p.getExpiryDate().toString() : null)
                .sku(p.getSku())
                .build();
    }

    private double resolveBaseUnits(Product product, String orgId, double quantity, String measureUnitCode) {
        if (measureUnitCode == null || measureUnitCode.isBlank()) {
            return quantity;
        }
        MeasureUnit mu = measureUnitRepository.findByOrganizationIdAndCode(orgId, measureUnitCode.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Unidad de medida no encontrada: " + measureUnitCode));
        return productUomService.toBaseUnits(product.getId(), mu.getId(), quantity);
    }

    private ConsumptionLog.Source resolveSource(String source) {
        if (source != null && source.toUpperCase().contains("WHATSAPP")) {
            return ConsumptionLog.Source.WHATSAPP;
        }
        return ConsumptionLog.Source.WEB;
    }
}
