package com.fenxiao.platform.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "platform_milestone_policy")
public class PlatformMilestonePolicy extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "platform_code", nullable = false, length = 32) private String platformCode;
    @Column(name = "guild_id", nullable = false, length = 64) private String guildId;
    @Column(name = "country_code", nullable = false, length = 10) private String countryCode;
    @Column(name = "minimum_withdrawable_amount", nullable = false, precision = 18, scale = 6) private BigDecimal minimumWithdrawableAmount;
    @Column(name = "currency_code", nullable = false, length = 16) private String currencyCode;
    @Column(name = "effective_from", nullable = false) private LocalDateTime effectiveFrom;
    @Column(name = "effective_to") private LocalDateTime effectiveTo;
    @Column(name = "enabled", nullable = false) private boolean enabled;

    protected PlatformMilestonePolicy() {}
    public static PlatformMilestonePolicy create(String platformCode, String guildId, String countryCode,
                                                 BigDecimal minimum, String currencyCode,
                                                 LocalDateTime effectiveFrom) {
        if (minimum == null || minimum.signum() < 0) throw new IllegalArgumentException("minimum withdrawable amount is invalid");
        PlatformMilestonePolicy value = new PlatformMilestonePolicy();
        value.platformCode = platformCode.trim().toUpperCase(Locale.ROOT);
        value.guildId = guildId.trim();
        value.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        value.minimumWithdrawableAmount = minimum;
        value.currencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);
        value.effectiveFrom = effectiveFrom;
        value.enabled = true;
        return value;
    }
    public void closeAt(LocalDateTime at) { this.effectiveTo = at; this.enabled = false; }
    public Long getId() { return id; }
    public String getPlatformCode() { return platformCode; }
    public String getGuildId() { return guildId; }
    public String getCountryCode() { return countryCode; }
    public BigDecimal getMinimumWithdrawableAmount() { return minimumWithdrawableAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
}
