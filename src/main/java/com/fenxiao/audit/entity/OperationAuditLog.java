package com.fenxiao.audit.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
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
}
