package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.AuditLogListItem;
import com.fenxiao.admin.api.dto.AuditLogListResponse;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogQueryService {

    private final OperationAuditLogRepository operationAuditLogRepository;

    public AuditLogQueryService(OperationAuditLogRepository operationAuditLogRepository) {
        this.operationAuditLogRepository = operationAuditLogRepository;
    }

    public AuditLogListResponse getAuditLogs(String moduleName, int page, int size) {
        validatePageRequest(page, size);
        Page<OperationAuditLog> auditLogs = operationAuditLogRepository.findAdminAuditLogs(moduleName, PageRequest.of(page, size));
        List<AuditLogListItem> items = auditLogs.getContent().stream()
                .map(log -> new AuditLogListItem(
                        log.getId(),
                        log.getModuleName(),
                        log.getTargetType(),
                        log.getTargetId(),
                        log.getActionName(),
                        log.getOperatorRole(),
                        log.getOperatorId(),
                        log.getRequestIp(),
                        log.getRemark(),
                        log.getOperatedAt()
                ))
                .toList();
        return new AuditLogListResponse(items, auditLogs.getTotalElements(), page, size);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
