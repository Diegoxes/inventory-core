package com.smarthome.contracts.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalBusinessContext {
    private String orgName;
    private int productCount;
    private int lowStockCount;
    private double totalInventoryValue;
    private String catalogPreview;
}
