package com.fenxiao.distribution.api;

import com.fenxiao.distribution.api.dto.CreateProfileRequest;
import com.fenxiao.distribution.api.dto.ProfileResponse;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.user.entity.UserDistributionProfile;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    private final DistributionBindingService distributionBindingService;

    public DistributionController(DistributionBindingService distributionBindingService) {
        this.distributionBindingService = distributionBindingService;
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
    public ProfileResponse createProfile(@Valid @RequestBody CreateProfileRequest request) {
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
                profile.getLanguageCode()
        );
    }
}
