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

import java.time.Clock;
import java.time.LocalDateTime;

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

    protected RiskEvent() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRiskType() {
        return riskType;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public RiskStatus getRiskStatus() {
        return riskStatus;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public String getResultNote() {
        return resultNote;
    }

    public static RiskEvent create(Long userId, String riskType, Integer riskLevel, String detailJson) {
        RiskEvent event = new RiskEvent();
        event.userId = userId;
        event.riskType = riskType;
        event.riskLevel = riskLevel;
        event.riskStatus = RiskStatus.PENDING;
        event.detailJson = detailJson;
        event.detectedAt = LocalDateTime.now(Clock.systemUTC());
        return event;
    }

    public void markHandled(Long handledBy, LocalDateTime handledAt, String resultNote) {
        this.riskStatus = RiskStatus.HANDLED;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.resultNote = resultNote;
    }

    public void markIgnored(Long handledBy, LocalDateTime handledAt, String resultNote) {
        this.riskStatus = RiskStatus.IGNORED;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.resultNote = resultNote;
    }
}
