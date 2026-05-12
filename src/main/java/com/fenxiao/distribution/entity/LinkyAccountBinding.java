package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "linky_account_binding")
public class LinkyAccountBinding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "linky_account", nullable = false, unique = true, length = 32)
    private String linkyAccount;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "invite_code", length = 20)
    private String inviteCode;

    @Column(name = "guild_id", length = 64)
    private String guildId;

    @Column(name = "guild_name", length = 128)
    private String guildName;

    @Column(name = "guild_owner_group", length = 64)
    private String guildOwnerGroup;

    @Column(name = "guild_check_status", nullable = false, length = 32)
    private String guildCheckStatus;

    @Column(name = "registration_eligibility", nullable = false, length = 32)
    private String registrationEligibility;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "remark", length = 255)
    private String remark;

    protected LinkyAccountBinding() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getLinkyAccount() {
        return linkyAccount;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public String getGuildOwnerGroup() {
        return guildOwnerGroup;
    }

    public String getGuildCheckStatus() {
        return guildCheckStatus;
    }

    public String getRegistrationEligibility() {
        return registrationEligibility;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public Long getCheckedBy() {
        return checkedBy;
    }

    public String getRemark() {
        return remark;
    }

    public static LinkyAccountBinding createUnchecked(String linkyAccount) {
        LinkyAccountBinding binding = new LinkyAccountBinding();
        binding.linkyAccount = linkyAccount;
        binding.guildCheckStatus = "PENDING";
        binding.registrationEligibility = "INELIGIBLE";
        return binding;
    }

    public void markEligible(String guildId, String guildName, Long checkedBy, String remark) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.guildOwnerGroup = "OURS";
        this.guildCheckStatus = "MATCHED_OURS";
        this.registrationEligibility = "ELIGIBLE";
        this.checkedAt = LocalDateTime.now(Clock.systemUTC());
        this.checkedBy = checkedBy;
        this.remark = remark;
    }

    public void markJoinedOtherGuild(String guildId, String guildName, Long checkedBy, String remark) {
        this.guildId = guildId;
        this.guildName = guildName;
        this.guildOwnerGroup = "OTHER";
        this.guildCheckStatus = "JOINED_OTHER_GUILD";
        this.registrationEligibility = "INELIGIBLE";
        this.checkedAt = LocalDateTime.now(Clock.systemUTC());
        this.checkedBy = checkedBy;
        this.remark = remark;
    }

    public void markNotJoined(Long checkedBy, String remark) {
        this.guildId = null;
        this.guildName = null;
        this.guildOwnerGroup = "NONE";
        this.guildCheckStatus = "NOT_JOINED";
        this.registrationEligibility = "INELIGIBLE";
        this.checkedAt = LocalDateTime.now(Clock.systemUTC());
        this.checkedBy = checkedBy;
        this.remark = remark;
    }

    public void attachRegistration(Long userId, String phoneNumber, String inviteCode) {
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.inviteCode = inviteCode;
    }
}
