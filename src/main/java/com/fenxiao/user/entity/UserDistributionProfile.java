package com.fenxiao.user.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.distribution.domain.DistributionRole;
import com.fenxiao.distribution.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_distribution_profile")
public class UserDistributionProfile extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "invite_code", nullable = false, unique = true, length = 20)
    private String inviteCode;

    @Column(name = "api_access_token", length = 64, unique = true)
    private String apiAccessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_role", nullable = false, length = 32)
    private DistributionRole distributionRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 32)
    private UserStatus userStatus;

    @Column(name = "is_effective_user", nullable = false)
    private boolean effectiveUser;

    @Column(name = "confirmed_income_total", nullable = false, precision = 18, scale = 6)
    private BigDecimal confirmedIncomeTotal;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    protected UserDistributionProfile() {
    }

    public Long getUserId() {
        return userId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getApiAccessToken() {
        return apiAccessToken;
    }

    public DistributionRole getDistributionRole() {
        return distributionRole;
    }

    public UserStatus getUserStatus() {
        return userStatus;
    }

    public boolean isEffectiveUser() {
        return effectiveUser;
    }

    public BigDecimal getConfirmedIncomeTotal() {
        return confirmedIncomeTotal;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public static UserDistributionProfile create(Long userId, String countryCode, String languageCode, String inviteCode) {
        UserDistributionProfile profile = new UserDistributionProfile();
        profile.userId = userId;
        profile.countryCode = countryCode;
        profile.languageCode = languageCode;
        profile.inviteCode = inviteCode;
        profile.apiAccessToken = UUID.randomUUID().toString().replace("-", "");
        profile.distributionRole = DistributionRole.NORMAL_USER;
        profile.userStatus = UserStatus.NORMAL;
        profile.effectiveUser = false;
        profile.confirmedIncomeTotal = BigDecimal.ZERO;
        profile.registeredAt = LocalDateTime.now(Clock.systemUTC());
        return profile;
    }

    public void addConfirmedIncome(BigDecimal incomeAmount) {
        this.confirmedIncomeTotal = this.confirmedIncomeTotal.add(incomeAmount);
        if (this.confirmedIncomeTotal.compareTo(BigDecimal.ZERO) > 0) {
            this.effectiveUser = true;
        }
    }

    public void markAsRiskUser() {
        this.userStatus = UserStatus.RISK;
    }
}
