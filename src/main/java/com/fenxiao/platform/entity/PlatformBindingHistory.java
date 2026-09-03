package com.fenxiao.platform.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.platform.domain.PlatformBindingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_binding_history")
public class PlatformBindingHistory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "binding_id", nullable = false) private Long bindingId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "platform_code", nullable = false, length = 32) private String platformCode;
    @Column(name = "platform_user_id", nullable = false, length = 64) private String platformUserId;
    @Column(name = "from_status", length = 32) private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 32) private String toStatus;
    @Column(name = "reason_code", length = 64) private String reasonCode;
    @Column(name = "reason_detail", length = 255) private String reasonDetail;
    @Column(name = "source_system", nullable = false, length = 64) private String sourceSystem;
    @Column(name = "operator_id") private Long operatorId;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;

    protected PlatformBindingHistory() {}

    public static PlatformBindingHistory record(PlatformAccountBinding binding, PlatformBindingStatus from,
                                                String reasonCode, String reasonDetail, String sourceSystem,
                                                Long operatorId, LocalDateTime at) {
        PlatformBindingHistory value = new PlatformBindingHistory();
        value.bindingId = binding.getId();
        value.userId = binding.getUserId();
        value.platformCode = binding.getPlatformCode();
        value.platformUserId = binding.getPlatformUserId();
        value.fromStatus = from == null ? null : from.name();
        value.toStatus = binding.getBindingStatus().name();
        value.reasonCode = reasonCode;
        value.reasonDetail = reasonDetail;
        value.sourceSystem = sourceSystem;
        value.operatorId = operatorId;
        value.occurredAt = at;
        return value;
    }
}

