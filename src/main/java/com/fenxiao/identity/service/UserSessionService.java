package com.fenxiao.identity.service;

import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.identity.domain.AccountStatus;
import com.fenxiao.identity.entity.UserSession;
import com.fenxiao.identity.repository.UserSessionRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class UserSessionService {
    private final UserSessionRepository sessionRepository;
    private final UserDistributionProfileRepository profileRepository;
    private final long ttlMinutes;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public UserSessionService(UserSessionRepository sessionRepository,
                              UserDistributionProfileRepository profileRepository,
                              @Value("${app.distribution.user-session-ttl-minutes:1440}") long ttlMinutes,
                              Clock clock) {
        this.sessionRepository = sessionRepository;
        this.profileRepository = profileRepository;
        this.ttlMinutes = ttlMinutes;
        this.clock = clock;
    }

    public IssuedSession issue(Long userId) {
        UserDistributionProfile profile = requireActiveProfile(userId);
        String token = newToken();
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(ttlMinutes);
        sessionRepository.save(UserSession.issue(profile.getUserId(), hash(token), profile.getSessionVersion(), expiresAt));
        return new IssuedSession(token, expiresAt);
    }

    public Long assertAccess(Long expectedUserId, String token) {
        UserSession session = requireUsable(token);
        if (!session.getUserId().equals(expectedUserId)) {
            throw new ForbiddenException("distribution access denied");
        }
        UserDistributionProfile profile = requireActiveProfile(expectedUserId);
        if (session.getSessionVersion() != profile.getSessionVersion()) {
            throw new ForbiddenException("distribution access denied");
        }
        session.touch(LocalDateTime.now(clock));
        return expectedUserId;
    }

    public IssuedSession refresh(String token) {
        UserSession current = requireUsable(token);
        UserDistributionProfile profile = requireActiveProfile(current.getUserId());
        if (current.getSessionVersion() != profile.getSessionVersion()) {
            throw new ForbiddenException("distribution access denied");
        }
        current.revoke(LocalDateTime.now(clock));
        return issue(current.getUserId());
    }

    public void revoke(String token) {
        UserSession session = requireUsable(token);
        session.revoke(LocalDateTime.now(clock));
    }

    public void revokeAll(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        sessionRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(session -> session.revoke(now));
    }

    private UserSession requireUsable(String token) {
        if (token == null || token.isBlank()) throw new ForbiddenException("distribution access denied");
        LocalDateTime now = LocalDateTime.now(clock);
        return sessionRepository.findByTokenHash(hash(token))
                .filter(session -> session.isUsableAt(now))
                .orElseThrow(() -> new ForbiddenException("distribution access denied"));
    }

    private UserDistributionProfile requireActiveProfile(Long userId) {
        UserDistributionProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("distribution access denied"));
        if (profile.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ForbiddenException("account is not active");
        }
        return profile;
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash session token", exception);
        }
    }

    public record IssuedSession(String accessToken, LocalDateTime expiresAt) {}
}
