package com.fenxiao.rule.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
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
}
