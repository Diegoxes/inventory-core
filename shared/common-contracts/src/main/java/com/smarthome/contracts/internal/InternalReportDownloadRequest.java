package com.smarthome.contracts.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalReportDownloadRequest {
    private String organizationId;
    private String fileName;
    private String contentType;
    /** Base64 del archivo */
    private String dataBase64;
}
