package com.fenxiao.risk.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.risk.domain.RiskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "risk_event")
public class RiskEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "risk_type", nullable = false, length = 64)
    private String riskType;

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status", nullable = false, length = 32)
    private RiskStatus riskStatus;

    @Lob
    @Column(name = "detail_json")
    private String detailJson;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Column(name = "result_note", length = 255)
    private String resultNote;
}
