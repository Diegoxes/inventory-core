package com.smarthome.contracts.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalRestockRequest {
    private String orgId;
    private String userId;
    private String productId;
    private double quantity;
    private String measureUnitCode;
    private String source;
    /** Crear producto nuevo cuando productId está vacío. */
    private String productName;
    private String unitType;
}
