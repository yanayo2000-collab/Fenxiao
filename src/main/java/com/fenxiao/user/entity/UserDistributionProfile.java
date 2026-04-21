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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
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
}
