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

    protected AdminAccount() {
    }

    public static AdminAccount create(String username, String displayName, String role, String passwordHash, boolean enabled) {
        AdminAccount account = new AdminAccount();
        account.username = normalizeUsername(username);
        account.displayName = displayName == null || displayName.isBlank() ? account.username : displayName.trim();
        account.role = role == null || role.isBlank() ? "operator" : role.trim().toLowerCase();
        account.passwordHash = passwordHash;
        account.enabled = enabled;
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

    public void recordLogin(LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return username.trim().toLowerCase();
    }
}
