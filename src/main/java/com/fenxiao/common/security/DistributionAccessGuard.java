package com.fenxiao.common.security;

import com.fenxiao.admin.service.AdminPermission;
import com.fenxiao.admin.service.AdminSessionService;
import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.identity.service.UserSessionService;
import com.fenxiao.identity.domain.AccountStatus;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class DistributionAccessGuard {

    private final String internalToken;
    private final String adminToken;
    private final String profileCreateToken;
    private final String linkySigningSecret;
    private final long linkyReplayWindowSeconds;
    private final AdminSessionService adminSessionService;
    private final UserSessionService userSessionService;
    private final UserDistributionProfileRepository userProfileRepository;
    private final boolean legacyUserTokenEnabled;
    private final Clock clock;

    public DistributionAccessGuard(@Value("${app.distribution.internal-token:}") String internalToken,
                                   @Value("${app.admin.token:}") String adminToken,
                                   @Value("${app.distribution.profile-create-token:}") String profileCreateToken,
                                   @Value("${app.distribution.linky-signing-secret:}") String linkySigningSecret,
                                   @Value("${app.distribution.linky-replay-window-seconds:900}") long linkyReplayWindowSeconds,
                                   @Value("${app.distribution.legacy-user-token-enabled:false}") boolean legacyUserTokenEnabled,
                                   UserDistributionProfileRepository userProfileRepository,
                                   AdminSessionService adminSessionService,
                                   UserSessionService userSessionService,
                                   Clock clock) {
        this.internalToken = internalToken;
        this.adminToken = adminToken;
        this.profileCreateToken = profileCreateToken;
        this.linkySigningSecret = linkySigningSecret;
        this.linkyReplayWindowSeconds = linkyReplayWindowSeconds;
        this.legacyUserTokenEnabled = legacyUserTokenEnabled;
        this.userProfileRepository = userProfileRepository;
        this.adminSessionService = adminSessionService;
        this.userSessionService = userSessionService;
        this.clock = clock;
    }

    public void assertUserAccess(Long targetUserId, String accessToken) {
        try {
            userSessionService.assertAccess(targetUserId, accessToken);
        } catch (ForbiddenException exception) {
            if (!legacyUserTokenEnabled) throw exception;
            var profile = userProfileRepository.findById(targetUserId)
                    .filter(value -> value.getAccountStatus() == AccountStatus.ACTIVE)
                    .filter(value -> value.getApiAccessToken() != null && value.getApiAccessToken().equals(accessToken))
                    .orElseThrow(() -> exception);
        }
    }

    public void assertProfileCreateToken(String token) {
        if (profileCreateToken == null || profileCreateToken.isBlank() || token == null || !MessageDigest.isEqual(profileCreateToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("profile create token invalid");
        }
    }

    public void assertInternalToken(String token) {
        InternalTokenCheckResult result = inspectInternalToken(token);
        if (!result.allowed()) {
            throw new ForbiddenException(result.message());
        }
    }

    public InternalTokenCheckResult inspectInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank() || token == null || !MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return new InternalTokenCheckResult(false, "INVALID", "internal token invalid");
        }
        return new InternalTokenCheckResult(true, "VALID", null);
    }

    public void assertLinkySignature(String timestamp,
                                     String signature,
                                     String linkyOrderId,
                                     Long userId,
                                     String incomeAmount,
                                     String currencyCode,
                                     String paidAt) {
        LinkyRequestCheckResult result = inspectLinkySignature(timestamp, signature, linkyOrderId, userId, incomeAmount, currencyCode, paidAt);
        if (!result.allowed()) {
            throw new ForbiddenException(result.message());
        }
    }

    public LinkyRequestCheckResult inspectLinkySignature(String timestamp,
                                                         String signature,
                                                         String linkyOrderId,
                                                         Long userId,
                                                         String incomeAmount,
                                                         String currencyCode,
                                                         String paidAt) {
        if (linkySigningSecret == null || linkySigningSecret.isBlank()) {
            return new LinkyRequestCheckResult(false, "NOT_CONFIGURED", "NOT_CHECKED", "linky signature verification not configured");
        }
        if (timestamp == null || timestamp.isBlank()) {
            return new LinkyRequestCheckResult(false, "NOT_CHECKED", "INVALID_TIMESTAMP", "linky signature invalid");
        }
        if (signature == null || signature.isBlank()) {
            return new LinkyRequestCheckResult(false, "INVALID", "NOT_CHECKED", "linky signature invalid");
        }
        Instant requestAt;
        try {
            requestAt = Instant.parse(timestamp);
        } catch (Exception exception) {
            return new LinkyRequestCheckResult(false, "NOT_CHECKED", "INVALID_TIMESTAMP", "linky signature invalid");
        }
        long skewSeconds = Math.abs(Duration.between(requestAt, Instant.now(clock)).getSeconds());
        if (skewSeconds > linkyReplayWindowSeconds) {
            return new LinkyRequestCheckResult(false, "VALID", "EXPIRED", "linky request expired");
        }
        String payload = timestamp + "." + linkyOrderId.trim() + "." + userId + "." + incomeAmount + "." + currencyCode.trim().toUpperCase() + "." + paidAt;
        String expected = sign(payload, linkySigningSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            return new LinkyRequestCheckResult(false, "INVALID", "VALID", "linky signature invalid");
        }
        return new LinkyRequestCheckResult(true, "VALID", "VALID", null);
    }

    public AdminSessionService.AdminPrincipal assertAdminAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.READ);
    }

    public AdminSessionService.AdminPrincipal assertAdminWriteAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.WRITE);
    }

    public AdminSessionService.AdminPrincipal assertAdminAccountManageAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.ACCOUNT_MANAGE);
    }

    public AdminSessionService.AdminPrincipal assertMentorManageAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.MENTOR_MANAGE);
    }

    public AdminSessionService.AdminPrincipal assertTeamManageAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.TEAM_MANAGE);
    }

    public AdminSessionService.AdminPrincipal assertFinanceAccess(String token, String sessionToken) {
        return adminSessionService.assertPermission(sessionToken, AdminPermission.FINANCE);
    }

    public AdminSessionService.AdminPrincipal assertAdminScopedAccess(String token,String sessionToken,String platform,String guild,String region){
        var principal=assertAdminAccess(token,sessionToken);principal.requireScope(platform,guild,region);return principal;
    }

    public AdminSessionService.AdminPrincipal assertAdminScopedWriteAccess(String token,String sessionToken,String platform,String guild,String region){
        var principal=assertAdminWriteAccess(token,sessionToken);principal.requireScope(platform,guild,region);return principal;
    }

    public void assertAdminToken(String token) {
        if (adminToken == null || adminToken.isBlank() || token == null || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("admin token invalid");
        }
    }

    private String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to verify linky signature", exception);
        }
    }

    public record InternalTokenCheckResult(boolean allowed, String status, String message) {
    }

    public record LinkyRequestCheckResult(boolean allowed, String signatureStatus, String replayStatus, String message) {
    }
}
