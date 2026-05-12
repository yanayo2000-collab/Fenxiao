package com.fenxiao.distribution.api;

import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.api.dto.CreateInviteBindingRequest;
import com.fenxiao.distribution.api.dto.CreateProfileRequest;
import com.fenxiao.distribution.api.dto.DistributionHomeResponse;
import com.fenxiao.distribution.api.dto.InviteBindingResponse;
import com.fenxiao.distribution.api.dto.IssueInviteCodeRequest;
import com.fenxiao.distribution.api.dto.IssueInviteCodeResponse;
import com.fenxiao.distribution.api.dto.PhoneCodeRequest;
import com.fenxiao.distribution.api.dto.PhoneLoginRequest;
import com.fenxiao.distribution.api.dto.ProfileResponse;
import com.fenxiao.distribution.api.dto.TeamListResponse;
import com.fenxiao.distribution.api.dto.WeeklyIncomeStatsResponse;
import com.fenxiao.distribution.api.dto.WithdrawRequestResponse;
import com.fenxiao.distribution.entity.InviteBindingRegistration;
import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.service.DistributionFrontendService;
import com.fenxiao.distribution.service.InviteBindingRegistrationService;
import com.fenxiao.distribution.service.InviteCodeIssueService;
import com.fenxiao.distribution.service.PhoneAuthService;
import com.fenxiao.distribution.service.WithdrawRequestService;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.api.dto.RewardSummaryResponse;
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
    private final InviteBindingRegistrationService inviteBindingRegistrationService;
    private final InviteCodeIssueService inviteCodeIssueService;
    private final WithdrawRequestService withdrawRequestService;
    private final PhoneAuthService phoneAuthService;
    private final DistributionAccessGuard distributionAccessGuard;

    public DistributionController(DistributionBindingService distributionBindingService,
                                  DistributionFrontendService distributionFrontendService,
                                  InviteBindingRegistrationService inviteBindingRegistrationService,
                                  InviteCodeIssueService inviteCodeIssueService,
                                  WithdrawRequestService withdrawRequestService,
                                  PhoneAuthService phoneAuthService,
                                  DistributionAccessGuard distributionAccessGuard) {
        this.distributionBindingService = distributionBindingService;
        this.distributionFrontendService = distributionFrontendService;
        this.inviteBindingRegistrationService = inviteBindingRegistrationService;
        this.inviteCodeIssueService = inviteCodeIssueService;
        this.withdrawRequestService = withdrawRequestService;
        this.phoneAuthService = phoneAuthService;
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

    @PostMapping("/bindings/register")
    public InviteBindingResponse registerInviteBinding(@Valid @RequestBody CreateInviteBindingRequest request) {
        InviteBindingRegistration registration = inviteBindingRegistrationService.register(request);
        return new InviteBindingResponse(
                registration.getId(),
                registration.getProductCode(),
                registration.getInviterUserId(),
                registration.getInviteCode(),
                registration.getWhatsappNumber(),
                registration.getLinkyAccount(),
                registration.getBindStatus(),
                registration.getSubmittedAt().toString()
        );
    }

    @PostMapping("/invite-codes/issue")
    public IssueInviteCodeResponse issueInviteCode(@Valid @RequestBody IssueInviteCodeRequest request) {
        InviteCodeIssueService.InviteCodeIssueResult result = inviteCodeIssueService.issue(request);
        return new IssueInviteCodeResponse(
                result.profile().getUserId(),
                result.record().getProductCode(),
                result.record().getWhatsappNumber(),
                result.record().getAppAccount(),
                result.profile().getInviteCode(),
                result.profile().getCountryCode(),
                result.profile().getLanguageCode(),
                result.profile().getApiAccessToken(),
                result.record().getIssuedAt().toString()
        );
    }


    @PostMapping("/auth/phone-codes")
    public Map<String, Object> issuePhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        return Map.of("phoneNumber", request.phoneNumber(), "verificationCode", phoneAuthService.issueCode(request.phoneNumber()), "ttlMinutes", 10);
    }

    @PostMapping("/auth/phone-login")
    public ProfileResponse phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        UserDistributionProfile profile = phoneAuthService.login(request);
        return new ProfileResponse(profile.getUserId(), profile.getInviteCode(), profile.getCountryCode(), profile.getLanguageCode(), profile.getApiAccessToken());
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


    @GetMapping("/rewards/{userId}/summary")
    public RewardSummaryResponse rewardSummary(@RequestHeader("X-Distribution-Token") String accessToken,
                                               @PathVariable Long userId) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        return distributionFrontendService.getRewardSummary(userId);
    }

    @GetMapping("/income-stats/weekly/{userId}")
    public WeeklyIncomeStatsResponse weeklyStats(@RequestHeader("X-Distribution-Token") String accessToken,
                                                 @PathVariable Long userId) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        return distributionFrontendService.getWeeklyStats(userId);
    }

    @GetMapping("/withdraw-requests/{userId}")
    public Map<String, Object> withdrawHistory(@RequestHeader("X-Distribution-Token") String accessToken,
                                               @PathVariable Long userId,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        var requestPage = withdrawRequestService.listRequests(userId, status, page, size);
        return Map.of("items", requestPage.getContent().stream().map(this::toWithdrawResponse).toList(), "total", requestPage.getTotalElements(), "page", page, "size", size);
    }

    @PostMapping("/withdraw-requests/{userId}")
    public WithdrawRequestResponse createWithdrawRequest(@RequestHeader("X-Distribution-Token") String accessToken,
                                                         @PathVariable Long userId) {
        distributionAccessGuard.assertUserAccess(userId, accessToken);
        WithdrawRequest request = withdrawRequestService.createRequest(userId);
        return new WithdrawRequestResponse(
                request.getRequestNo(),
                request.getUserId(),
                request.getRequestedDiamondAmount(),
                request.getRequestStatus(),
                request.getRequestWeek(),
                request.getRequestedAt().toString()
        );
    }
    private WithdrawRequestResponse toWithdrawResponse(WithdrawRequest request) {
        return new WithdrawRequestResponse(
                request.getRequestNo(),
                request.getUserId(),
                request.getRequestedDiamondAmount(),
                request.getRequestStatus(),
                request.getRequestWeek(),
                request.getRequestedAt().toString()
        );
    }
}
