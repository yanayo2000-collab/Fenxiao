package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.AuditLogListResponse;
import com.fenxiao.admin.api.dto.LinkyEligibilityCheckRequest;
import com.fenxiao.admin.api.dto.LinkyEligibilityCheckResponse;
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
import com.fenxiao.admin.api.dto.WithdrawRequestActionRequest;
import com.fenxiao.admin.api.dto.WithdrawRequestItemResponse;
import com.fenxiao.admin.api.dto.WithdrawRequestListResponse;
import com.fenxiao.distribution.api.dto.GuildConfigRequest;
import com.fenxiao.distribution.api.dto.GuildConfigResponse;
import com.fenxiao.distribution.api.dto.GuildWeeklyReportResponse;
import com.fenxiao.distribution.api.dto.LinkyBatchRefreshResponse;
import com.fenxiao.admin.service.AuditLogQueryService;
import com.fenxiao.admin.service.DistributionReportService;
import com.fenxiao.admin.service.LinkyReplayRecordService;
import com.fenxiao.admin.service.LinkyWebhookLogService;
import com.fenxiao.admin.service.OwnershipAdminService;
import com.fenxiao.admin.service.RelationAdjustmentService;
import com.fenxiao.admin.service.RiskEventActionService;
import com.fenxiao.admin.service.RiskEventQueryService;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.entity.LinkyAccountBinding;
import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.distribution.service.GuildAccountConfigService;
import com.fenxiao.distribution.service.LinkyRegistrationEligibilityService;
import com.fenxiao.distribution.service.WithdrawRequestService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    private final LinkyRegistrationEligibilityService linkyRegistrationEligibilityService;
    private final WithdrawRequestService withdrawRequestService;
    private final GuildAccountConfigService guildAccountConfigService;
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
                                       LinkyRegistrationEligibilityService linkyRegistrationEligibilityService,
                                       WithdrawRequestService withdrawRequestService,
                                       GuildAccountConfigService guildAccountConfigService,
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
        this.linkyRegistrationEligibilityService = linkyRegistrationEligibilityService;
        this.withdrawRequestService = withdrawRequestService;
        this.guildAccountConfigService = guildAccountConfigService;
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

    @PostMapping("/linky-eligibility-checks/{linkyAccount}/refresh")
    public LinkyEligibilityCheckResponse refreshLinkyEligibility(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                 @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                                 @PathVariable String linkyAccount) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        LinkyAccountBinding binding = linkyRegistrationEligibilityService.refreshEligibilityFromProbe(linkyAccount);
        return toLinkyEligibilityCheckResponse(binding);
    }

    @PostMapping("/linky-eligibility-checks")
    public LinkyEligibilityCheckResponse upsertLinkyEligibility(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                                @RequestBody LinkyEligibilityCheckRequest request) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        LinkyAccountBinding binding = switch ((request.result() == null ? "" : request.result().trim().toUpperCase())) {
            case "MATCHED_OURS" -> linkyRegistrationEligibilityService.markEligible(request.linkyAccount(), request.guildId(), request.guildName(), 0L, request.remark());
            case "JOINED_OTHER_GUILD" -> linkyRegistrationEligibilityService.markJoinedOtherGuild(request.linkyAccount(), request.guildId(), request.guildName(), 0L, request.remark());
            case "NOT_JOINED" -> linkyRegistrationEligibilityService.markNotJoined(request.linkyAccount(), 0L, request.remark());
            default -> throw new IllegalArgumentException("unsupported guild check result");
        };
        return toLinkyEligibilityCheckResponse(binding);
    }


    @PostMapping("/linky-eligibility-checks/batch-refresh")
    public LinkyBatchRefreshResponse batchRefreshLinkyEligibility(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                  @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        LinkyRegistrationEligibilityService.BatchRefreshResult result = linkyRegistrationEligibilityService.refreshAllEligibility();
        return new LinkyBatchRefreshResponse(result.successCount(), result.failureCount());
    }

    @GetMapping("/guild-configs")
    public java.util.List<GuildConfigResponse> listGuildConfigs(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                                @RequestParam(defaultValue = "LINKY") String product) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return guildAccountConfigService.list(product).stream().map(c -> new GuildConfigResponse(c.getId(), c.getProductCode(), c.getInviterUserId(), c.getGuildId(), c.getGuildName(), c.getGuildInviteCode(), c.isEnabled())).toList();
    }

    @PostMapping("/guild-configs")
    public GuildConfigResponse upsertGuildConfig(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                 @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                 @RequestBody GuildConfigRequest request) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        var c = guildAccountConfigService.upsert(request);
        return new GuildConfigResponse(c.getId(), c.getProductCode(), c.getInviterUserId(), c.getGuildId(), c.getGuildName(), c.getGuildInviteCode(), c.isEnabled());
    }

    @GetMapping("/guild-configs/{guildId}/weekly-report")
    public GuildWeeklyReportResponse guildWeeklyReport(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                       @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                       @PathVariable String guildId,
                                                       @RequestParam(defaultValue = "LINKY") String product,
                                                       @RequestParam(defaultValue = "CURRENT") String week) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        return guildAccountConfigService.weeklyReport(product, guildId, week);
    }

    @GetMapping("/guild-configs/{guildId}/weekly-report/export")
    public ResponseEntity<byte[]> exportGuildWeeklyReport(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                          @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                          @PathVariable String guildId,
                                                          @RequestParam(defaultValue = "LINKY") String product,
                                                          @RequestParam(defaultValue = "CURRENT") String week) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        GuildWeeklyReportResponse report = guildAccountConfigService.weeklyReport(product, guildId, week);
        String csv = "productCode,guildId,week,registeredUsers,incomeAmount,rewardAmount\n"
                + report.productCode() + "," + report.guildId() + "," + report.week() + ","
                + report.registeredUsers() + "," + report.incomeAmount() + "," + report.rewardAmount() + "\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guild-weekly-report.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/withdraw-requests")
    public WithdrawRequestListResponse listWithdrawRequests(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                            @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                            @RequestParam(required = false) Long userId,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        var requestPage = withdrawRequestService.listRequests(userId, status, page, size);
        var items = requestPage.getContent().stream()
                .map(this::toWithdrawRequestItemResponse)
                .toList();
        return new WithdrawRequestListResponse(items, requestPage.getTotalElements(), page, size);
    }

    @PostMapping("/withdraw-requests/{requestNo}/approve")
    public WithdrawRequestItemResponse approveWithdrawRequest(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                              @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                              @PathVariable String requestNo,
                                                              @RequestBody(required = false) WithdrawRequestActionRequest request) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        WithdrawRequest withdrawRequest = withdrawRequestService.approveRequest(requestNo, 0L, request == null ? null : request.remark());
        return toWithdrawRequestItemResponse(withdrawRequest);
    }

    @PostMapping("/withdraw-requests/{requestNo}/reject")
    public WithdrawRequestItemResponse rejectWithdrawRequest(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                             @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                             @PathVariable String requestNo,
                                                             @RequestBody(required = false) WithdrawRequestActionRequest request) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        WithdrawRequest withdrawRequest = withdrawRequestService.rejectRequest(requestNo, 0L, request == null ? null : request.remark());
        return toWithdrawRequestItemResponse(withdrawRequest);
    }


    @GetMapping("/withdraw-requests/export")
    public ResponseEntity<byte[]> exportWithdrawRequests(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                         @RequestHeader(value = "X-Admin-Session", required = false) String adminSessionToken,
                                                         @RequestParam(required = false) Long userId,
                                                         @RequestParam(required = false) String status) {
        distributionAccessGuard.assertAdminAccess(adminToken, adminSessionToken);
        var requestPage = withdrawRequestService.listRequests(userId, status, 0, 100);
        StringBuilder csv = new StringBuilder("requestNo,userId,amount,status,week,requestedAt\n");
        requestPage.getContent().forEach(r -> csv.append(r.getRequestNo()).append(',').append(r.getUserId()).append(',').append(r.getRequestedDiamondAmount()).append(',').append(r.getRequestStatus()).append(',').append(r.getRequestWeek()).append(',').append(r.getRequestedAt()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=withdraw-requests.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private WithdrawRequestItemResponse toWithdrawRequestItemResponse(WithdrawRequest request) {
        return new WithdrawRequestItemResponse(
                request.getRequestNo(),
                request.getUserId(),
                request.getRequestedDiamondAmount(),
                request.getRequestStatus(),
                request.getRequestWeek(),
                request.getRequestedAt().toString()
        );
    }

    private LinkyEligibilityCheckResponse toLinkyEligibilityCheckResponse(LinkyAccountBinding binding) {
        return new LinkyEligibilityCheckResponse(
                binding.getLinkyAccount(),
                binding.getGuildId(),
                binding.getGuildName(),
                binding.getGuildCheckStatus(),
                binding.getRegistrationEligibility(),
                binding.getCheckedAt() == null ? null : binding.getCheckedAt().toString(),
                binding.getRemark()
        );
    }
}
