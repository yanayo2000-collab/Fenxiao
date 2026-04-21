package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.OverviewReportResponse;
import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.admin.api.dto.RiskEventListResponse;
import com.fenxiao.admin.service.DistributionReportService;
import com.fenxiao.admin.service.RiskEventQueryService;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import com.fenxiao.risk.domain.RiskStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/admin/distribution")
public class DistributionAdminController {

    private final RewardCalculationService rewardCalculationService;
    private final DistributionQueryService distributionQueryService;
    private final DistributionReportService distributionReportService;
    private final RiskEventQueryService riskEventQueryService;
    private final DistributionAccessGuard distributionAccessGuard;

    public DistributionAdminController(RewardCalculationService rewardCalculationService,
                                       DistributionQueryService distributionQueryService,
                                       DistributionReportService distributionReportService,
                                       RiskEventQueryService riskEventQueryService,
                                       DistributionAccessGuard distributionAccessGuard) {
        this.rewardCalculationService = rewardCalculationService;
        this.distributionQueryService = distributionQueryService;
        this.distributionReportService = distributionReportService;
        this.riskEventQueryService = riskEventQueryService;
        this.distributionAccessGuard = distributionAccessGuard;
    }

    @GetMapping("/health")
    public Map<String, Object> health(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                      @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return Map.of(
                "module", "distribution-admin",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }

    @GetMapping("/relation/{userId}")
    public RelationDetailResponse relationDetail(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                 @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                 @PathVariable Long userId) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return distributionQueryService.getRelationDetail(userId);
    }

    @GetMapping("/rewards")
    public RewardListResponse listRewards(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                          @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                          @RequestParam(required = false) Long beneficiaryUserId,
                                          @RequestParam(required = false) RewardStatus status,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return rewardCalculationService.getRecentRewards(beneficiaryUserId, status, startAt, endAt, page, size);
    }

    @GetMapping("/risk-events")
    public RiskEventListResponse listRiskEvents(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) RiskStatus riskStatus,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return riskEventQueryService.getRiskEvents(userId, riskStatus, startAt, endAt, page, size);
    }

    @GetMapping("/reports/overview")
    public OverviewReportResponse overview(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                           @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return distributionReportService.getOverview();
    }
}
