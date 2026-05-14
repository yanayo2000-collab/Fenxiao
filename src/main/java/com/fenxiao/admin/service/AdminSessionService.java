package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.AdminSessionResponse;
import com.fenxiao.admin.entity.AdminAccount;
import com.fenxiao.admin.repository.AdminAccountRepository;
import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.common.api.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminSessionService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String sessionSecret;
    private final long sessionTtlMinutes;
    private final long loginWindowMinutes;
    private final int loginMaxAttempts;
    private final Clock clock;
    private final AdminAccountRepository adminAccountRepository;
    private final AdminPasswordHasher passwordHasher;
    private final ConcurrentHashMap<String, FailedLoginWindow> failedLoginWindows = new ConcurrentHashMap<>();

    @Autowired
    public AdminSessionService(@Value("${app.admin.session-secret:${app.admin.token:}}") String sessionSecret,
                               @Value("${app.admin.session-ttl-minutes:720}") long sessionTtlMinutes,
                               @Value("${app.admin.login-window-minutes:10}") long loginWindowMinutes,
                               @Value("${app.admin.login-max-attempts:5}") int loginMaxAttempts,
                               AdminAccountRepository adminAccountRepository,
                               AdminPasswordHasher passwordHasher) {
        this(sessionSecret, sessionTtlMinutes, loginWindowMinutes, loginMaxAttempts, Clock.systemUTC(), adminAccountRepository, passwordHasher);
    }

    AdminSessionService(String sessionSecret,
                        long sessionTtlMinutes,
                        long loginWindowMinutes,
                        int loginMaxAttempts,
                        Clock clock,
                        AdminAccountRepository adminAccountRepository,
                        AdminPasswordHasher passwordHasher) {
        this.sessionSecret = sessionSecret;
        this.sessionTtlMinutes = sessionTtlMinutes;
        this.loginWindowMinutes = loginWindowMinutes;
        this.loginMaxAttempts = loginMaxAttempts;
        this.clock = clock;
        this.adminAccountRepository = adminAccountRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public AdminSessionResponse createSession(String username, String password, String clientKey) {
        assertSessionSigningConfigured();
        String normalizedClientKey = normalizeClientKey(clientKey);
        enforceLoginRateLimit(normalizedClientKey);
        String normalizedUsername = normalizeUsername(username);
        AdminAccount account = adminAccountRepository.findByUsername(normalizedUsername)
                .filter(AdminAccount::isEnabled)
                .orElse(null);
        if (account == null || !passwordHasher.matches(password, account.getPasswordHash())) {
            recordFailedLogin(normalizedClientKey);
            throw new ForbiddenException("admin login invalid");
        }
        failedLoginWindows.remove(normalizedClientKey);
        Instant expiresAt = clock.instant().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        account.recordLogin(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        String payload = expiresAt.toEpochMilli() + ":" + account.getId() + ":" + account.getUsername() + ":" + account.getRole() + ":" + UUID.randomUUID();
        return new AdminSessionResponse(sign(payload), expiresAt.toString(), account.getUsername(), account.getDisplayName(), account.getRole());
    }

    public AdminPrincipal assertSession(String sessionToken) {
        assertSessionSigningConfigured();
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new ForbiddenException("admin session invalid");
        }

        String[] parts = sessionToken.split("\\.", 2);
        if (parts.length != 2) {
            throw new ForbiddenException("admin session invalid");
        }

        String payload = decode(parts[0]);
        String expectedSignature = hmacSha256(parts[0]);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("admin session invalid");
        }

        String[] payloadParts = payload.split(":", 5);
        if (payloadParts.length != 5) {
            throw new ForbiddenException("admin session invalid");
        }

        long expiresAtEpochMillis;
        long accountId;
        try {
            expiresAtEpochMillis = Long.parseLong(payloadParts[0]);
            accountId = Long.parseLong(payloadParts[1]);
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("admin session invalid");
        }

        if (Instant.ofEpochMilli(expiresAtEpochMillis).isBefore(clock.instant())) {
            throw new ForbiddenException("admin session expired");
        }
        return new AdminPrincipal(accountId, payloadParts[2], payloadParts[3]);
    }

    public AdminPrincipal assertPermission(String sessionToken, AdminPermission permission) {
        AdminPrincipal principal = assertSession(sessionToken);
        if (!permission.allows(principal.role())) {
            throw new ForbiddenException("admin permission denied");
        }
        return principal;
    }

    private void enforceLoginRateLimit(String clientKey) {
        FailedLoginWindow window = failedLoginWindows.get(clientKey);
        if (window == null) {
            return;
        }
        if (window.windowStart().plus(loginWindowMinutes, ChronoUnit.MINUTES).isBefore(clock.instant())) {
            failedLoginWindows.remove(clientKey);
            return;
        }
        if (window.failedAttempts() >= loginMaxAttempts) {
            throw new TooManyRequestsException("admin login rate limited");
        }
    }

    private void recordFailedLogin(String clientKey) {
        failedLoginWindows.compute(clientKey, (key, existing) -> {
            Instant now = clock.instant();
            if (existing == null || existing.windowStart().plus(loginWindowMinutes, ChronoUnit.MINUTES).isBefore(now)) {
                return new FailedLoginWindow(now, 1);
            }
            return new FailedLoginWindow(existing.windowStart(), existing.failedAttempts() + 1);
        });
    }

    private void assertSessionSigningConfigured() {
        if (sessionSecret == null || sessionSecret.isBlank()) {
            throw new ForbiddenException("admin auth not configured");
        }
    }

    private String normalizeClientKey(String clientKey) {
        return (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ForbiddenException("admin login invalid");
        }
        return username.trim().toLowerCase();
    }

    private String sign(String payload) {
        String encodedPayload = URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + hmacSha256(encodedPayload);
    }

    private String decode(String encodedPayload) {
        try {
            return new String(URL_DECODER.decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("admin session invalid");
        }
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("admin session signing failed");
        }
    }

    private record FailedLoginWindow(Instant windowStart, int failedAttempts) {
    }

    public record AdminPrincipal(long accountId, String username, String role) {
    }
}
