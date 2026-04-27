package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.AuditLogListResponse;
import com.fenxiao.admin.api.dto.LinkyReplayRecordListResponse;
import com.fenxiao.admin.api.dto.LinkyWebhookLogListResponse;
import com.fenxiao.admin.api.dto.ManualOwnershipCorrectionRequest;
import com.fenxiao.admin.api.dto.ManualRelationAdjustmentRequest;
import com.fenxiao.admin.api.dto.OverviewReportResponse;
import com.fenxiao.admin.api.dto.OwnershipDetailResponse;
import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.admin.api.dto.RiskEventActionRequest;
import com.fenxiao.admin.api.dto.RiskEventListItem;
import com.fenxiao.admin.api.dto.RiskEventListResponse;
import com.fenxiao.admin.service.AuditLogQueryService;
import com.fenxiao.admin.service.DistributionReportService;
import com.fenxiao.admin.service.LinkyReplayRecordService;
import com.fenxiao.admin.service.LinkyWebhookLogService;
import com.fenxiao.admin.service.OwnershipAdminService;
import com.fenxiao.admin.service.RelationAdjustmentService;
import com.fenxiao.admin.service.RiskEventActionService;
import com.fenxiao.admin.service.RiskEventQueryService;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import com.fenxiao.risk.domain.RiskStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final LinkyReplayRecordService linkyReplayRecordService;
    private final LinkyWebhookLogService linkyWebhookLogService;
    private final AuditLogQueryService auditLogQueryService;
    private final RiskEventQueryService riskEventQueryService;
    private final RiskEventActionService riskEventActionService;
    private final OwnershipAdminService ownershipAdminService;
    private final RelationAdjustmentService relationAdjustmentService;
    private final DistributionAccessGuard distributionAccessGuard;

    public DistributionAdminController(RewardCalculationService rewardCalculationService,
                                       DistributionQueryService distributionQueryService,
                                       DistributionReportService distributionReportService,
                                       LinkyReplayRecordService linkyReplayRecordService,
                                       LinkyWebhookLogService linkyWebhookLogService,
                                       AuditLogQueryService auditLogQueryService,
                                       RiskEventQueryService riskEventQueryService,
                                       RiskEventActionService riskEventActionService,
                                       OwnershipAdminService ownershipAdminService,
                                       RelationAdjustmentService relationAdjustmentService,
                                       DistributionAccessGuard distributionAccessGuard) {
        this.rewardCalculationService = rewardCalculationService;
        this.distributionQueryService = distributionQueryService;
        this.distributionReportService = distributionReportService;
        this.linkyReplayRecordService = linkyReplayRecordService;
        this.linkyWebhookLogService = linkyWebhookLogService;
        this.auditLogQueryService = auditLogQueryService;
        this.riskEventQueryService = riskEventQueryService;
        this.riskEventActionService = riskEventActionService;
        this.ownershipAdminService = ownershipAdminService;
        this.relationAdjustmentService = relationAdjustmentService;
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
                                                 @PathVariable Long userId,
                                                 @RequestParam(required = false) String product) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return distributionQueryService.getRelationDetail(userId, product);
    }

    @PostMapping("/relation/{userId}/adjustments")
    public RelationDetailResponse adjustRelation(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                 @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                 @PathVariable Long userId,
                                                 @RequestParam(required = false) String product,
                                                 @Valid @RequestBody ManualRelationAdjustmentRequest request,
                                                 HttpServletRequest httpServletRequest) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return relationAdjustmentService.adjustRelation(userId, request.level1InviterId(), request.note(), httpServletRequest.getRemoteAddr(), product);
    }

    @GetMapping("/ownership/{userId}")
    public OwnershipDetailResponse ownershipDetail(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                   @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                   @PathVariable Long userId) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return ownershipAdminService.getOwnership(userId);
    }

    @PostMapping("/ownership/{userId}/corrections")
    public OwnershipDetailResponse correctOwnership(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                    @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                    @PathVariable Long userId,
                                                    @Valid @RequestBody ManualOwnershipCorrectionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return ownershipAdminService.correctOwnership(userId, request.productCode(), request.note(), httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/rewards")
    public RewardListResponse listRewards(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                          @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                          @RequestParam(required = false) Long beneficiaryUserId,
                                          @RequestParam(required = false) RewardStatus status,
                                          @RequestParam(required = false) String product,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return rewardCalculationService.getRecentRewards(beneficiaryUserId, status, startAt, endAt, page, size, product);
    }

    @GetMapping("/risk-events")
    public RiskEventListResponse listRiskEvents(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) RiskStatus riskStatus,
                                                @RequestParam(required = false) String product,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return riskEventQueryService.getRiskEvents(userId, riskStatus, startAt, endAt, page, size, product);
    }

    @PostMapping("/risk-events/{riskEventId}/actions")
    public RiskEventListItem applyRiskEventAction(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                  @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                  @PathVariable Long riskEventId,
                                                  @Valid @RequestBody RiskEventActionRequest request,
                                                  HttpServletRequest httpServletRequest) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return riskEventActionService.applyAction(riskEventId, request.action(), request.note(), httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/audit-logs")
    public AuditLogListResponse listAuditLogs(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                              @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                              @RequestParam(required = false) String moduleName,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return auditLogQueryService.getAuditLogs(moduleName, page, size);
    }

    @GetMapping("/linky-webhook-logs")
    public LinkyWebhookLogListResponse listLinkyWebhookLogs(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                            @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                            @RequestParam(required = false) String linkyOrderId,
                                                            @RequestParam(required = false) Long userId,
                                                            @RequestParam(required = false) String requestStatus,
                                                            @RequestParam(required = false) String product,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return linkyWebhookLogService.getLogs(linkyOrderId, userId, requestStatus, page, size, product);
    }

    @GetMapping("/linky-replay-records")
    public LinkyReplayRecordListResponse listLinkyReplayRecords(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                                @RequestParam(required = false) String linkyOrderId,
                                                                @RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) String product,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return linkyReplayRecordService.getRecords(linkyOrderId, userId, page, size, product);
    }

    @GetMapping("/reports/overview")
    public OverviewReportResponse overview(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                           @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                           @RequestParam(required = false) String product) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return distributionReportService.getOverview(product);
    }
}
