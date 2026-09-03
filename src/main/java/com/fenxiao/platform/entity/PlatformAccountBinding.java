package com.fenxiao.platform.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.platform.domain.PlatformBindingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "platform_account_binding")
public class PlatformAccountBinding extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "platform_code", nullable = false, length = 32)
    private String platformCode;
    @Column(name = "platform_user_id", nullable = false, length = 64)
    private String platformUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "binding_status", nullable = false, length = 32)
    private PlatformBindingStatus bindingStatus;
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "official_guild_id", length = 64)
    private String officialGuildId;
    @Column(name = "official_joined_at")
    private LocalDateTime officialJoinedAt;
    @Column(name = "verification_source", length = 64)
    private String verificationSource;
    @Column(name = "verification_reference", length = 128)
    private String verificationReference;
    @Column(name = "rejection_code", length = 64)
    private String rejectionCode;
    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;
    @Column(name = "version_no", nullable = false)
    private int versionNo;

    protected PlatformAccountBinding() {}

    public static PlatformAccountBinding submit(Long userId, String platformCode, String platformUserId, LocalDateTime at) {
        PlatformAccountBinding value = new PlatformAccountBinding();
        value.userId = userId;
        value.platformCode = platformCode.trim().toUpperCase(Locale.ROOT);
        value.platformUserId = platformUserId.trim();
        value.bindingStatus = PlatformBindingStatus.SUBMITTED;
        value.submittedAt = at;
        value.versionNo = 1;
        return value;
    }

    public void startVerification() {
        requireStatus(PlatformBindingStatus.SUBMITTED, PlatformBindingStatus.REJECTED);
        bindingStatus = PlatformBindingStatus.VERIFYING;
        versionNo++;
    }

    public void verify(String guildId, LocalDateTime joinedAt, String source, String reference, LocalDateTime at) {
        requireStatus(PlatformBindingStatus.SUBMITTED, PlatformBindingStatus.VERIFYING, PlatformBindingStatus.REJECTED);
        bindingStatus = PlatformBindingStatus.VERIFIED;
        officialGuildId = required(guildId, "official guild id");
        officialJoinedAt = joinedAt;
        verificationSource = required(source, "verification source");
        verificationReference = trimToNull(reference);
        verifiedAt = at;
        rejectionCode = null;
        rejectionReason = null;
        versionNo++;
    }

    public void reject(String code, String reason, String source) {
        if (bindingStatus == PlatformBindingStatus.VERIFIED || bindingStatus == PlatformBindingStatus.UNBOUND) {
            throw new IllegalStateException("verified or historical binding cannot be overwritten");
        }
        bindingStatus = PlatformBindingStatus.REJECTED;
        rejectionCode = required(code, "rejection code");
        rejectionReason = required(reason, "rejection reason");
        verificationSource = required(source, "verification source");
        versionNo++;
    }

    private void requireStatus(PlatformBindingStatus... allowed) {
        for (PlatformBindingStatus status : allowed) if (bindingStatus == status) return;
        throw new IllegalStateException("platform binding transition is not allowed from " + bindingStatus);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPlatformCode() { return platformCode; }
    public String getPlatformUserId() { return platformUserId; }
    public PlatformBindingStatus getBindingStatus() { return bindingStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public String getOfficialGuildId() { return officialGuildId; }
    public LocalDateTime getOfficialJoinedAt() { return officialJoinedAt; }
    public String getRejectionCode() { return rejectionCode; }
    public String getRejectionReason() { return rejectionReason; }
    public int getVersionNo() { return versionNo; }
}

