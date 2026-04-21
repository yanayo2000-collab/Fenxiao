package com.fenxiao.audit.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_audit_log")
public class OperationAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_role", nullable = false, length = 32)
    private String operatorRole;

    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "action_name", nullable = false, length = 64)
    private String actionName;

    @Lob
    @Column(name = "before_data")
    private String beforeData;

    @Lob
    @Column(name = "after_data")
    private String afterData;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Column(name = "operated_at", nullable = false)
    private LocalDateTime operatedAt;

    @Column(name = "remark", length = 255)
    private String remark;

    protected OperationAuditLog() {
    }

    public Long getId() {
        return id;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getActionName() {
        return actionName;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public String getRemark() {
        return remark;
    }

    public static OperationAuditLog create(Long operatorId,
                                           String operatorRole,
                                           String moduleName,
                                           String targetType,
                                           Long targetId,
                                           String actionName,
                                           String beforeData,
                                           String afterData,
                                           String requestIp,
                                           String remark,
                                           LocalDateTime operatedAt) {
        OperationAuditLog log = new OperationAuditLog();
        log.operatorId = operatorId;
        log.operatorRole = operatorRole;
        log.moduleName = moduleName;
        log.targetType = targetType;
        log.targetId = targetId;
        log.actionName = actionName;
        log.beforeData = beforeData;
        log.afterData = afterData;
        log.requestIp = requestIp;
        log.operatedAt = operatedAt;
        log.remark = remark;
        return log;
    }
}
