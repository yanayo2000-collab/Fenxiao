package com.fenxiao.admin.api.dto;

import java.time.LocalDateTime;

public record AuditLogListItem(
        Long id,
        String moduleName,
        String targetType,
        Long targetId,
        String actionName,
        String operatorRole,
        Long operatorId,
        String requestIp,
        String remark,
        LocalDateTime operatedAt
) {
}
