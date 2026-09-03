package com.fenxiao.platform.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_lifecycle_snapshot")
public class PlatformLifecycleSnapshot extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "platform_code", nullable = false, length = 32) private String platformCode;
    @Column(name = "platform_user_id", nullable = false, length = 64) private String platformUserId;
    @Column(name = "lifecycle_start_date") private LocalDate lifecycleStartDate;
    @Column(name = "binding_verified", nullable = false) private boolean bindingVerified;
    @Column(name = "valid_72_hour_start", nullable = false) private boolean valid72HourStart;
    @Column(name = "first_income_at") private LocalDateTime firstIncomeAt;
    @Column(name = "first_withdraw_eligible_at") private LocalDateTime firstWithdrawEligibleAt;
    @Column(name = "consecutive_7_day_active", nullable = false) private boolean consecutive7DayActive;
    @Column(name = "consecutive_30_day_active", nullable = false) private boolean consecutive30DayActive;
    @Column(name = "consecutive_active_days", nullable = false) private int consecutiveActiveDays;
    @Column(name = "cumulative_net_income", nullable = false, precision = 18, scale = 6) private BigDecimal cumulativeNetIncome;
    @Column(name = "shadow_only", nullable = false) private boolean shadowOnly;
    @Column(name = "evaluated_at", nullable = false) private LocalDateTime evaluatedAt;

    protected PlatformLifecycleSnapshot() {}

    public static PlatformLifecycleSnapshot create(PlatformAccountBinding binding, boolean shadowOnly, LocalDateTime at) {
        PlatformLifecycleSnapshot value = new PlatformLifecycleSnapshot();
        value.userId = binding.getUserId(); value.platformCode = binding.getPlatformCode(); value.platformUserId = binding.getPlatformUserId();
        value.cumulativeNetIncome = BigDecimal.ZERO; value.shadowOnly = shadowOnly; value.evaluatedAt = at;
        return value;
    }

    public void evaluate(PlatformAccountBinding binding, boolean valid72HourStart, LocalDateTime firstIncomeAt, LocalDateTime firstWithdrawEligibleAt,
                         int consecutiveActiveDays, BigDecimal cumulativeNetIncome, boolean shadowOnly, LocalDateTime at) {
        this.lifecycleStartDate = binding.getOfficialJoinedAt() == null ? null : binding.getOfficialJoinedAt().toLocalDate();
        this.bindingVerified = binding.getBindingStatus() == com.fenxiao.platform.domain.PlatformBindingStatus.VERIFIED;
        this.valid72HourStart = valid72HourStart;
        this.firstIncomeAt = firstIncomeAt;
        this.firstWithdrawEligibleAt = firstWithdrawEligibleAt;
        this.consecutiveActiveDays = consecutiveActiveDays;
        this.consecutive7DayActive = consecutiveActiveDays >= 7;
        this.consecutive30DayActive = consecutiveActiveDays >= 30;
        this.cumulativeNetIncome = cumulativeNetIncome;
        this.shadowOnly = shadowOnly;
        this.evaluatedAt = at;
    }

    public boolean isBindingVerified() { return bindingVerified; }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPlatformCode() { return platformCode; }
    public String getPlatformUserId() { return platformUserId; }
    public boolean isValid72HourStart() { return valid72HourStart; }
    public LocalDateTime getFirstIncomeAt() { return firstIncomeAt; }
    public LocalDateTime getFirstWithdrawEligibleAt() { return firstWithdrawEligibleAt; }
    public boolean isConsecutive7DayActive() { return consecutive7DayActive; }
    public boolean isConsecutive30DayActive() { return consecutive30DayActive; }
    public int getConsecutiveActiveDays() { return consecutiveActiveDays; }
    public BigDecimal getCumulativeNetIncome() { return cumulativeNetIncome; }
    public boolean isShadowOnly() { return shadowOnly; }
}
