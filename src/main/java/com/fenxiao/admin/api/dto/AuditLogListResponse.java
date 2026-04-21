package com.fenxiao.admin.api.dto;

import java.util.List;

public record AuditLogListResponse(
        List<AuditLogListItem> items,
        long total,
        int page,
        int size
) {
}
