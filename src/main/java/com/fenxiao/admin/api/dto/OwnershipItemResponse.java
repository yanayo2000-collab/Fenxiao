package com.fenxiao.admin.api.dto;

import java.time.LocalDateTime;

public record OwnershipItemResponse(
        Long id,
        String productCode,
        String ownershipStatus,
        String ownershipSource,
        String sourceRecordType,
        Long sourceRecordId,
        LocalDateTime effectiveAt
) {
}
