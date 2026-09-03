package com.fenxiao.admin.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_account")
public class AdminAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "role", nullable = false, length = 32)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "platform_scope", nullable = false, length = 512)
    private String platformScope;

    @Column(name = "guild_scope", nullable = false, length = 1024)
    private String guildScope;

    @Column(name = "region_scope", nullable = false, length = 512)
    private String regionScope;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret", length = 256)
    private String mfaSecret;

    protected AdminAccount() {
    }

    public static AdminAccount create(String username, String displayName, String role, String passwordHash, boolean enabled) {
        AdminAccount account = new AdminAccount();
        account.username = normalizeUsername(username);
        account.displayName = displayName == null || displayName.isBlank() ? account.username : displayName.trim();
        account.role = role == null || role.isBlank() ? "operator" : role.trim().toLowerCase();
        account.passwordHash = passwordHash;
        account.enabled = enabled;
        account.passwordChangedAt = LocalDateTime.now();
        account.platformScope = "*";
        account.guildScope = "*";
        account.regionScope = "*";
        return account;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public LocalDateTime getLastFailedLoginAt() { return lastFailedLoginAt; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public String getPlatformScope() { return platformScope; }
    public String getGuildScope() { return guildScope; }
    public String getRegionScope() { return regionScope; }

    public void recordLogin(LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
    }

    public void updatePasswordHash(String passwordHash, boolean mustChangePassword, LocalDateTime changedAt) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
        this.passwordChangedAt = changedAt;
    }

    public void updateProfile(String displayName, String role, String platformScope, String guildScope, String regionScope) {
        this.displayName = displayName == null || displayName.isBlank() ? username : displayName.trim();
        this.role = role.trim().toLowerCase();
        this.platformScope = normalizeScope(platformScope);
        this.guildScope = normalizeScope(guildScope);
        this.regionScope = normalizeScope(regionScope);
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void recordFailedLogin(LocalDateTime at, int maxAttempts, long lockMinutes) {
        this.failedLoginCount++;
        this.lastFailedLoginAt = at;
        if (failedLoginCount >= maxAttempts) this.lockedUntil = at.plusMinutes(lockMinutes);
    }

    public void clearLoginFailures() {
        this.failedLoginCount = 0;
        this.lastFailedLoginAt = null;
        this.lockedUntil = null;
    }

    public boolean isLockedAt(LocalDateTime at) {
        return lockedUntil != null && lockedUntil.isAfter(at);
    }


    private static String normalizeScope(String value) {
        return value == null || value.isBlank() ? "*" : value.trim().toUpperCase();
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return username.trim().toLowerCase();
    }
}
