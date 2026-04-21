package com.fenxiao.reward.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.reward.domain.RewardStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_record")
public class RewardRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_event_id", nullable = false, length = 64)
    private String sourceEventId;

    @Column(name = "beneficiary_user_id", nullable = false)
    private Long beneficiaryUserId;

    @Column(name = "source_user_id", nullable = false)
    private Long sourceUserId;

    @Column(name = "reward_level", nullable = false)
    private Integer rewardLevel;

    @Column(name = "income_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal incomeAmount;

    @Column(name = "reward_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal rewardRate;

    @Column(name = "reward_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal rewardAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_status", nullable = false, length = 32)
    private RewardStatus rewardStatus;

    @Column(name = "unfreeze_at")
    private LocalDateTime unfreezeAt;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "risk_flag", nullable = false)
    private boolean riskFlag;

    @Column(name = "risk_reason", length = 255)
    private String riskReason;

    protected RewardRecord() {
    }

    public Long getId() {
        return id;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Long getBeneficiaryUserId() {
        return beneficiaryUserId;
    }

    public Long getSourceUserId() {
        return sourceUserId;
    }

    public Integer getRewardLevel() {
        return rewardLevel;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public BigDecimal getRewardRate() {
        return rewardRate;
    }

    public BigDecimal getRewardAmount() {
        return rewardAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public RewardStatus getRewardStatus() {
        return rewardStatus;
    }

    public LocalDateTime getUnfreezeAt() {
        return unfreezeAt;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public boolean isRiskFlag() {
        return riskFlag;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public static RewardRecord create(String sourceEventId,
                                      Long beneficiaryUserId,
                                      Long sourceUserId,
                                      Integer rewardLevel,
                                      BigDecimal incomeAmount,
                                      BigDecimal rewardRate,
                                      BigDecimal rewardAmount,
                                      String currencyCode,
                                      Integer freezeDays,
                                      LocalDateTime eventTime) {
        RewardRecord record = new RewardRecord();
        record.sourceEventId = sourceEventId;
        record.beneficiaryUserId = beneficiaryUserId;
        record.sourceUserId = sourceUserId;
        record.rewardLevel = rewardLevel;
        record.incomeAmount = incomeAmount;
        record.rewardRate = rewardRate;
        record.rewardAmount = rewardAmount;
        record.currencyCode = currencyCode;
        record.rewardStatus = RewardStatus.FROZEN;
        record.calculatedAt = eventTime;
        record.unfreezeAt = eventTime.plusDays(freezeDays);
        record.riskFlag = false;
        return record;
    }

    public void markRiskHold(String riskReason) {
        this.rewardStatus = RewardStatus.RISK_HOLD;
        this.riskFlag = true;
        this.riskReason = riskReason;
    }

    public void markAvailable() {
        this.rewardStatus = RewardStatus.AVAILABLE;
    }
}
