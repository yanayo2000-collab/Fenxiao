package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.OverviewReportResponse;
import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.admin.service.DistributionReportService;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/distribution")
public class DistributionAdminController {

    private final RewardCalculationService rewardCalculationService;
    private final DistributionQueryService distributionQueryService;
    private final DistributionReportService distributionReportService;
    private final DistributionAccessGuard distributionAccessGuard;

    public DistributionAdminController(RewardCalculationService rewardCalculationService,
                                       DistributionQueryService distributionQueryService,
                                       DistributionReportService distributionReportService,
                                       DistributionAccessGuard distributionAccessGuard) {
        this.rewardCalculationService = rewardCalculationService;
        this.distributionQueryService = distributionQueryService;
        this.distributionReportService = distributionReportService;
        this.distributionAccessGuard = distributionAccessGuard;
    }

    @GetMapping("/health")
    public Map<String, Object> health(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        distributionAccessGuard.assertAdminToken(adminToken);
        return Map.of(
                "module", "distribution-admin",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }

    @GetMapping("/relation/{userId}")
    public RelationDetailResponse relationDetail(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                 @PathVariable Long userId) {
        distributionAccessGuard.assertAdminToken(adminToken);
        return distributionQueryService.getRelationDetail(userId);
    }

    @GetMapping("/rewards")
    public RewardListResponse listRewards(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                          @RequestParam(required = false) Long beneficiaryUserId,
                                          @RequestParam(required = false) RewardStatus status) {
        distributionAccessGuard.assertAdminToken(adminToken);
        return rewardCalculationService.getRecentRewards(beneficiaryUserId, status);
    }

    @GetMapping("/reports/overview")
    public OverviewReportResponse overview(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        distributionAccessGuard.assertAdminToken(adminToken);
        return distributionReportService.getOverview();
    }
}
