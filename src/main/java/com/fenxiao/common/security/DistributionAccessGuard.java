package com.fenxiao.common.security;

import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DistributionAccessGuard {

    private final String internalToken;
    private final UserDistributionProfileRepository userDistributionProfileRepository;

    public DistributionAccessGuard(@Value("${app.distribution.internal-token:}") String internalToken,
                                   UserDistributionProfileRepository userDistributionProfileRepository) {
        this.internalToken = internalToken;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
    }

    public void assertUserAccess(Long targetUserId, String accessToken) {
        UserDistributionProfile profile = userDistributionProfileRepository.findById(targetUserId)
                .orElseThrow(() -> new ForbiddenException("distribution access denied"));
        if (accessToken == null || accessToken.isBlank() || profile.getApiAccessToken() == null || !profile.getApiAccessToken().equals(accessToken)) {
            throw new ForbiddenException("distribution access denied");
        }
    }

    public void assertInternalToken(String token) {
        if (internalToken == null || internalToken.isBlank() || token == null || !java.security.MessageDigest.isEqual(internalToken.getBytes(), token.getBytes())) {
            throw new ForbiddenException("internal token invalid");
        }
    }
}
