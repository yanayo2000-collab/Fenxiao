package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.distribution.domain.BindSource;
import com.fenxiao.distribution.domain.LockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_relation")
public class DistributionRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "level1_inviter_id")
    private Long level1InviterId;

    @Column(name = "level2_inviter_id")
    private Long level2InviterId;

    @Column(name = "level3_inviter_id")
    private Long level3InviterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bind_source", nullable = false, length = 32)
    private BindSource bindSource;

    @Column(name = "bind_time", nullable = false)
    private LocalDateTime bindTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status", nullable = false, length = 32)
    private LockStatus lockStatus;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "cross_country", nullable = false)
    private boolean crossCountry;

    protected DistributionRelation() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getLevel1InviterId() {
        return level1InviterId;
    }

    public Long getLevel2InviterId() {
        return level2InviterId;
    }

    public Long getLevel3InviterId() {
        return level3InviterId;
    }

    public BindSource getBindSource() {
        return bindSource;
    }

    public LocalDateTime getBindTime() {
        return bindTime;
    }

    public LockStatus getLockStatus() {
        return lockStatus;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isCrossCountry() {
        return crossCountry;
    }

    public static DistributionRelation createRoot(Long userId, String countryCode) {
        DistributionRelation relation = new DistributionRelation();
        relation.userId = userId;
        relation.bindSource = BindSource.MANUAL;
        relation.bindTime = LocalDateTime.now(Clock.systemUTC());
        relation.lockStatus = LockStatus.UNLOCKED;
        relation.countryCode = countryCode;
        relation.crossCountry = false;
        return relation;
    }

    public static DistributionRelation createBound(Long userId,
                                                   String countryCode,
                                                   BindSource bindSource,
                                                   Long level1InviterId,
                                                   Long level2InviterId,
                                                   Long level3InviterId,
                                                   boolean crossCountry) {
        DistributionRelation relation = new DistributionRelation();
        relation.userId = userId;
        relation.level1InviterId = level1InviterId;
        relation.level2InviterId = level2InviterId;
        relation.level3InviterId = level3InviterId;
        relation.bindSource = bindSource;
        relation.bindTime = LocalDateTime.now(Clock.systemUTC());
        relation.lockStatus = LockStatus.UNLOCKED;
        relation.countryCode = countryCode;
        relation.crossCountry = crossCountry;
        return relation;
    }

    public void lock() {
        this.lockStatus = LockStatus.LOCKED;
        this.lockTime = LocalDateTime.now(Clock.systemUTC());
    }

    public void lock(LocalDateTime lockTime) {
        this.lockStatus = LockStatus.LOCKED;
        this.lockTime = lockTime;
    }

    public void unlock() {
        this.lockStatus = LockStatus.UNLOCKED;
        this.lockTime = null;
    }

    public void rebindManually(Long level1InviterId,
                               Long level2InviterId,
                               Long level3InviterId,
                               boolean crossCountry,
                               LocalDateTime bindTime) {
        this.level1InviterId = level1InviterId;
        this.level2InviterId = level2InviterId;
        this.level3InviterId = level3InviterId;
        this.bindSource = BindSource.MANUAL;
        this.crossCountry = crossCountry;
        this.bindTime = bindTime;
    }
}
