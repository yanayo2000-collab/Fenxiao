package com.fenxiao.common.security;

import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class DistributionAccessGuard {

    private final String internalToken;
    private final String adminToken;
    private final String profileCreateToken;
    private final UserDistributionProfileRepository userDistributionProfileRepository;

    public DistributionAccessGuard(@Value("${app.distribution.internal-token:}") String internalToken,
                                   @Value("${app.admin.token:}") String adminToken,
                                   @Value("${app.distribution.profile-create-token:}") String profileCreateToken,
                                   UserDistributionProfileRepository userDistributionProfileRepository) {
        this.internalToken = internalToken;
        this.adminToken = adminToken;
        this.profileCreateToken = profileCreateToken;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
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

    public void assertAdminToken(String token) {
        if (adminToken == null || adminToken.isBlank() || token == null || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("admin token invalid");
        }
    }
}
