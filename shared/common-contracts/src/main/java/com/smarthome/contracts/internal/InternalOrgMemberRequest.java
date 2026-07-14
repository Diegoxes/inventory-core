package com.smarthome.contracts.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalOrgMemberRequest {
    private String userId;
    private String organizationId;
    /** MANAGER, MEMBER o VIEWER */
    private String orgRole;
}
