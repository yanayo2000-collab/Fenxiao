package com.fenxiao.common.security;

import com.fenxiao.admin.service.AdminSessionService;
import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class DistributionAccessGuard {

    private final String internalToken;
    private final String adminToken;
    private final String profileCreateToken;
    private final String linkySigningSecret;
    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final AdminSessionService adminSessionService;

    public DistributionAccessGuard(@Value("${app.distribution.internal-token:}") String internalToken,
                                   @Value("${app.admin.token:}") String adminToken,
                                   @Value("${app.distribution.profile-create-token:}") String profileCreateToken,
                                   @Value("${app.distribution.linky-signing-secret:}") String linkySigningSecret,
                                   UserDistributionProfileRepository userDistributionProfileRepository,
                                   AdminSessionService adminSessionService) {
        this.internalToken = internalToken;
        this.adminToken = adminToken;
        this.profileCreateToken = profileCreateToken;
        this.linkySigningSecret = linkySigningSecret;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.adminSessionService = adminSessionService;
    }

    public void assertUserAccess(Long targetUserId, String accessToken) {
        UserDistributionProfile profile = userDistributionProfileRepository.findById(targetUserId)
                .orElseThrow(() -> new ForbiddenException("distribution access denied"));
        if (accessToken == null || accessToken.isBlank() || profile.getApiAccessToken() == null || !profile.getApiAccessToken().equals(accessToken)) {
            throw new ForbiddenException("distribution access denied");
        }
    }

    public void assertProfileCreateToken(String token) {
        if (profileCreateToken == null || profileCreateToken.isBlank() || token == null || !MessageDigest.isEqual(profileCreateToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("profile create token invalid");
        }
    }

    public void assertInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank() || token == null || !MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("internal token invalid");
        }
    }

    public void assertLinkySignature(String timestamp,
                                     String signature,
                                     String linkyOrderId,
                                     Long userId,
                                     String incomeAmount,
                                     String currencyCode,
                                     String paidAt) {
        if (linkySigningSecret == null || linkySigningSecret.isBlank()) {
            return;
        }
        if (timestamp == null || timestamp.isBlank() || signature == null || signature.isBlank()) {
            throw new ForbiddenException("linky signature invalid");
        }
        String payload = timestamp + "." + linkyOrderId.trim() + "." + userId + "." + incomeAmount + "." + currencyCode.trim().toUpperCase() + "." + paidAt;
        String expected = sign(payload, linkySigningSecret);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("linky signature invalid");
        }
    }

    public void assertAdminAccess(String token, String sessionToken) {
        adminSessionService.assertSession(sessionToken);
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
}
