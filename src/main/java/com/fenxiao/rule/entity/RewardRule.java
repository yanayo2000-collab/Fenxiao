package com.fenxiao.rule.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_rule")
public class RewardRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "reward_level", nullable = false)
    private Integer rewardLevel;

    @Column(name = "reward_rate", nullable = false, precision = 8, scale = 6)
    private BigDecimal rewardRate;

    @Column(name = "freeze_days", nullable = false)
    private Integer freezeDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_by")
    private Long createdBy;

    protected RewardRule() {
    }

    public Long getId() {
        return id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Integer getRewardLevel() {
        return rewardLevel;
    }

    public BigDecimal getRewardRate() {
        return rewardRate;
    }

    public Integer getFreezeDays() {
        return freezeDays;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public String getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public static RewardRule create(String countryCode,
                                    String roleCode,
                                    Integer rewardLevel,
                                    BigDecimal rewardRate,
                                    Integer freezeDays,
                                    Long createdBy) {
        return create(countryCode, roleCode, rewardLevel, rewardRate, freezeDays, createdBy, LocalDateTime.now(Clock.systemUTC()).minusMinutes(1), null);
    }

    public static RewardRule create(String countryCode,
                                    String roleCode,
                                    Integer rewardLevel,
                                    BigDecimal rewardRate,
                                    Integer freezeDays,
                                    Long createdBy,
                                    LocalDateTime effectiveFrom,
                                    LocalDateTime effectiveTo) {
        RewardRule rule = new RewardRule();
        rule.countryCode = countryCode;
        rule.roleCode = roleCode;
        rule.rewardLevel = rewardLevel;
        rule.rewardRate = rewardRate;
        rule.freezeDays = freezeDays;
        rule.effectiveFrom = effectiveFrom;
        rule.effectiveTo = effectiveTo;
        rule.status = "ACTIVE";
        rule.createdBy = createdBy;
        return rule;
    }
}
