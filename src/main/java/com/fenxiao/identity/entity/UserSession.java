package com.fenxiao.identity.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_session")
public class UserSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    protected UserSession() {}

    public static UserSession issue(Long userId, String tokenHash, long sessionVersion, LocalDateTime expiresAt) {
        UserSession session = new UserSession();
        session.userId = userId;
        session.tokenHash = tokenHash;
        session.sessionVersion = sessionVersion;
        session.expiresAt = expiresAt;
        return session;
    }

    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public long getSessionVersion() { return sessionVersion; }
    public LocalDateTime getRevokedAt() { return revokedAt; }

    public boolean isUsableAt(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void touch(LocalDateTime now) { this.lastSeenAt = now; }
    public void revoke(LocalDateTime now) { this.revokedAt = now; }
}
