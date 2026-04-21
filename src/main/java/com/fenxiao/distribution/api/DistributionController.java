package com.fenxiao.distribution.api;

import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.api.dto.CreateProfileRequest;
import com.fenxiao.distribution.api.dto.DistributionHomeResponse;
import com.fenxiao.distribution.api.dto.ProfileResponse;
import com.fenxiao.distribution.api.dto.TeamListResponse;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.service.DistributionFrontendService;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.user.entity.UserDistributionProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    private final DistributionBindingService distributionBindingService;
    private final DistributionFrontendService distributionFrontendService;
    private final DistributionAccessGuard distributionAccessGuard;

    public DistributionController(DistributionBindingService distributionBindingService,
                                  DistributionFrontendService distributionFrontendService,
                                  DistributionAccessGuard distributionAccessGuard) {
        this.distributionBindingService = distributionBindingService;
        this.distributionFrontendService = distributionFrontendService;
        this.distributionAccessGuard = distributionAccessGuard;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "module", "distribution",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }

    @PostMapping("/profiles")
    public ProfileResponse createProfile(@RequestHeader(value = "X-Profile-Create-Token", required = false) String profileCreateToken,
                                         @Valid @RequestBody CreateProfileRequest request) {
        distributionAccessGuard.assertProfileCreateToken(profileCreateToken);
        UserDistributionProfile profile = distributionBindingService.createProfile(
                request.userId(),
                request.countryCode(),
                request.languageCode(),
                request.inviteCode()
        );
        return new ProfileResponse(
                profile.getUserId(),
                profile.getInviteCode(),
                profile.getCountryCode(),
                profile.getLanguageCode(),
                profile.getApiAccessToken()
        );
    }

    @GetMapping("/home/{userId}")
    public DistributionHomeResponse home(@RequestHeader("X-Distribution-Token") String accessToken,
                                         @PathVariable Long userId) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        return distributionFrontendService.getHome(userId);
    }

    @GetMapping("/team/{userId}")
    public TeamListResponse team(@RequestHeader("X-Distribution-Token") String accessToken,
                                 @PathVariable Long userId) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        return distributionFrontendService.getDirectTeam(userId);
    }

    @GetMapping("/rewards/{userId}")
    public RewardListResponse rewardDetails(@RequestHeader("X-Distribution-Token") String accessToken,
                                            @PathVariable Long userId,
                                            @RequestParam(required = false) RewardStatus status) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        return distributionFrontendService.getRewardDetails(userId, status);
    }
}
