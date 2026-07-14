package com.smarthome.contracts.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalProductSummary {
    private String id;
    private String name;
    private String category;
    private double currentStock;
    private String unitType;
    private Double minStock;
    private String expiryDate;
    private String sku;
}
