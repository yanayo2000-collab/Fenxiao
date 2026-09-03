package com.fenxiao.reward.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.domain.RewardType;
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

    public static final String CURRENT_ENGINE_VERSION = "BANDEIRA_V1_DIRECT_ONLY";

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

    @Column(name = "withdraw_status", nullable = false, length = 32)
    private String withdrawStatus;

    @Column(name = "reward_engine_version", nullable = false, length = 32)
    private String rewardEngineVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 32)
    private RewardType rewardType;

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

    public String getWithdrawStatus() {
        return withdrawStatus;
    }

    public String getRewardEngineVersion() {
        return rewardEngineVersion;
    }

    public RewardType getRewardType() {
        return rewardType;
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
        return create(sourceEventId, beneficiaryUserId, sourceUserId, rewardLevel, incomeAmount, rewardRate,
                rewardAmount, currencyCode, freezeDays, eventTime, CURRENT_ENGINE_VERSION, RewardType.DIRECT_RECRUIT);
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
                                      LocalDateTime eventTime,
                                      String rewardEngineVersion,
                                      RewardType rewardType) {
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
        record.withdrawStatus = "UNCLAIMED";
        record.rewardEngineVersion = rewardEngineVersion;
        record.rewardType = rewardType;
        return record;
    }

    public void markRiskHold(String riskReason) {
        assertMutableDirectReward();
        this.rewardStatus = RewardStatus.RISK_HOLD;
        this.riskFlag = true;
        this.riskReason = riskReason;
    }

    public void markAvailable() {
        assertMutableDirectReward();
        this.rewardStatus = RewardStatus.AVAILABLE;
    }

    public void releaseFromRiskHold(LocalDateTime now) {
        assertMutableDirectReward();
        this.riskFlag = false;
        this.riskReason = null;
        if (this.unfreezeAt != null && this.unfreezeAt.isAfter(now)) {
            this.rewardStatus = RewardStatus.FROZEN;
            return;
        }
        this.rewardStatus = RewardStatus.AVAILABLE;
    }

    public void markClaimedInRequest() {
        assertMutableDirectReward();
        this.withdrawStatus = "CLAIMED_IN_REQUEST";
    }

    public void markPaidOut() {
        assertMutableDirectReward();
        this.withdrawStatus = "PAID_OUT";
    }

    public void markPaidOutFromExistingRequest() {
        assertExistingRequestLifecycleAllowed();
        this.withdrawStatus = "PAID_OUT";
    }

    public void resetWithdrawClaim() {
        assertMutableDirectReward();
        this.withdrawStatus = "UNCLAIMED";
    }

    public void resetWithdrawClaimFromExistingRequest() {
        assertExistingRequestLifecycleAllowed();
        this.withdrawStatus = "UNCLAIMED";
    }

    public boolean isMutableDirectReward() {
        return rewardType == RewardType.DIRECT_RECRUIT
                && Integer.valueOf(1).equals(rewardLevel);
    }

    private void assertMutableDirectReward() {
        if (!isMutableDirectReward()) {
            throw new IllegalStateException("legacy or multilevel reward is read-only");
        }
    }

    private void assertExistingRequestLifecycleAllowed() {
        if (!"CLAIMED_IN_REQUEST".equals(withdrawStatus)) {
            throw new IllegalStateException("reward is not claimed in an existing withdraw request");
        }
        if (!isMutableDirectReward() && !"LEGACY_V0".equals(rewardEngineVersion)) {
            throw new IllegalStateException("new multilevel reward cannot be settled");
        }
    }
}
