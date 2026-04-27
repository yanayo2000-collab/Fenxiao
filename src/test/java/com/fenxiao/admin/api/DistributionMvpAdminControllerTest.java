package com.fenxiao.admin.api;

import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.distribution.api.dto.IssueInviteCodeRequest;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.service.InviteCodeIssueService;
import com.fenxiao.linky.repository.LinkyWebhookLogRepository;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.reward.service.RewardCalculationService;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@SpringBootTest(properties = {
        "app.admin.token=test-admin-token",
        "app.distribution.internal-token=test-token",
        "app.distribution.linky-signing-secret=test-linky-secret",
        "app.distribution.linky-replay-window-seconds=900"
})
class DistributionMvpAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private InviteCodeIssueService inviteCodeIssueService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Autowired
    private UserDistributionProfileRepository userDistributionProfileRepository;

    @Autowired
    private DistributionRelationRepository distributionRelationRepository;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private OperationAuditLogRepository operationAuditLogRepository;

    @Autowired
    private LinkyWebhookLogRepository linkyWebhookLogRepository;

    @Autowired
    private com.fenxiao.distribution.repository.UserProductOwnershipRepository userProductOwnershipRepository;

    @Test
    void shouldReturnRelationDetailForUser() throws Exception {
        String rootCode = distributionBindingService.createProfile(10001L, "ID", "id", null).getInviteCode();
        String level1Code = distributionBindingService.createProfile(10002L, "ID", "id", rootCode).getInviteCode();
        distributionBindingService.createProfile(10003L, "ID", "id", level1Code);

        mockMvc.perform(get("/admin/distribution/relation/10003")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10003))
                .andExpect(jsonPath("$.level1InviterId").value(10002))
                .andExpect(jsonPath("$.level2InviterId").value(10001))
                .andExpect(jsonPath("$.lockStatus").value("UNLOCKED"));
    }

    @Test
    void shouldRestrictRelationDetailByProductCode() throws Exception {
        String linkyRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****5101", "10101")).record().getInviteCode();
        distributionBindingService.createProfile(10102L, "ID", "id", linkyRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****5102", "10102"));

        String toolRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628****5201", "10201")).record().getInviteCode();
        distributionBindingService.createProfile(10202L, "ID", "id", toolRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628****5202", "10202"));

        String adminSessionToken = loginAsAdmin();

        mockMvc.perform(get("/admin/distribution/relation/10102")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10102));

        mockMvc.perform(get("/admin/distribution/relation/10102")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "TOOLX")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("distribution relation not found for product"));
    }

    @Test
    void shouldFilterRewardsWithPaginationAndReturnOverviewReport() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(11001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(11002L, "ID", "id", rootCode);
        distributionBindingService.createProfile(11003L, "ID", "id", rootCode);
        rewardCalculationService.processIncomeEvent("evt-report-1", 11002L, new BigDecimal("80.00"), "USD", LocalDateTime.of(2026, 4, 21, 10, 0));
        rewardCalculationService.processIncomeEvent("evt-report-2", 11003L, new BigDecimal("60.00"), "USD", LocalDateTime.of(2026, 4, 21, 11, 0));

        String adminSessionToken = loginAsAdmin();

        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("beneficiaryUserId", "11001")
                        .param("startAt", "2026-04-21T10:30:00")
                        .param("endAt", "2026-04-21T11:30:00")
                        .param("page", "0")
                        .param("size", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].beneficiaryUserId").value(11001))
                .andExpect(jsonPath("$.items[0].sourceUserId").value(11003))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));

        mockMvc.perform(get("/admin/distribution/reports/overview")
                        .header("X-Admin-Session", adminSessionToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedUsers").value(2))
                .andExpect(jsonPath("$.effectiveUsers").value(2))
                .andExpect(jsonPath("$.rewardTotal").value(21.0))
                .andExpect(jsonPath("$.frozenRewardTotal").value(21.0));
    }

    @Test
    void shouldFilterOverviewRewardsAndRiskEventsByProductCode() throws Exception {
        seedRules();
        String linkyRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628111111111", "21001")).record().getInviteCode();
        distributionBindingService.createProfile(21002L, "ID", "id", linkyRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628111111112", "21002"));

        String toolRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628222222221", "22001")).record().getInviteCode();
        distributionBindingService.createProfile(22002L, "ID", "id", toolRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628222222222", "22002"));

        UserDistributionProfile linkyRiskUser = userDistributionProfileRepository.findById(21002L).orElseThrow();
        linkyRiskUser.markAsRiskUser();
        userDistributionProfileRepository.save(linkyRiskUser);

        rewardCalculationService.processIncomeEvent("evt-linky-product-1", 21002L, new BigDecimal("100.00"), "USD", LocalDateTime.of(2026, 4, 22, 10, 0));
        rewardCalculationService.processIncomeEvent("evt-toolx-product-1", 22002L, new BigDecimal("200.00"), "USD", LocalDateTime.of(2026, 4, 22, 11, 0));

        String adminSessionToken = loginAsAdmin();

        mockMvc.perform(get("/admin/distribution/reports/overview")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitedUsers").value(1))
                .andExpect(jsonPath("$.effectiveUsers").value(1))
                .andExpect(jsonPath("$.rewardTotal").value(15.0))
                .andExpect(jsonPath("$.riskEventCount").value(1));

        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].beneficiaryUserId").value(21001))
                .andExpect(jsonPath("$.items[0].sourceUserId").value(21002));

        mockMvc.perform(get("/admin/distribution/risk-events")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(21002));
    }

    @Test
    void shouldReturnRiskEventsForRiskUser() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(13001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(13002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(13002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);

        rewardCalculationService.processIncomeEvent("evt-risk-list-1", 13002L, new BigDecimal("90.00"), "USD", LocalDateTime.now());

        mockMvc.perform(get("/admin/distribution/risk-events")
                        .header("X-Admin-Session", loginAsAdmin())
                        .param("userId", "13002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userId").value(13002))
                .andExpect(jsonPath("$.items[0].riskType").value("USER_STATUS_RISK"))
                .andExpect(jsonPath("$.items[0].riskStatus").value("PENDING"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldPutRiskRewardsOnHoldForRiskUser() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(12001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(12002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(12002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);

        rewardCalculationService.processIncomeEvent("evt-risk-1", 12002L, new BigDecimal("50.00"), "USD", LocalDateTime.now());

        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Session", loginAsAdmin())
                        .param("status", "RISK_HOLD")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].rewardStatus").value("RISK_HOLD"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldFreezeRiskUserAndWriteAuditLog() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(14001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(14002L, "ID", "id", rootCode);
        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(14002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-freeze-1", 14002L, new BigDecimal("80.00"), "USD", LocalDateTime.now().minusDays(1));
        Long riskEventId = riskEventRepository.findAdminRiskEvents(14002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "FREEZE_USER",
                                  "note": "confirmed suspicious behavior"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskStatus").value("HANDLED"))
                .andExpect(jsonPath("$.resultNote").value("confirmed suspicious behavior"));

        UserDistributionProfile profile = userDistributionProfileRepository.findById(14002L).orElseThrow();
        DistributionRelation relation = distributionRelationRepository.findByUserId(14002L).orElseThrow();
        RewardRecord rewardRecord = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-freeze-1").getFirst();

        org.assertj.core.api.Assertions.assertThat(profile.getUserStatus().name()).isEqualTo("RISK");
        org.assertj.core.api.Assertions.assertThat(relation.getLockStatus().name()).isEqualTo("LOCKED");
        org.assertj.core.api.Assertions.assertThat(rewardRecord.getRewardStatus().name()).isEqualTo("RISK_HOLD");
        org.assertj.core.api.Assertions.assertThat(operationAuditLogRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/admin/distribution/risk-events")
                        .header("X-Admin-Session", loginAsAdmin())
                        .param("userId", "14002")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].handledBy").value(0))
                .andExpect(jsonPath("$.items[0].handledAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].resultNote").value("confirmed suspicious behavior"));
    }

    @Test
    void shouldUnfreezeRiskUserAndRestoreRewards() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(15001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(15002L, "ID", "id", rootCode);
        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(15002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-unfreeze-admin-1", 15002L, new BigDecimal("120.00"), "USD", LocalDateTime.now().minusDays(10));
        Long riskEventId = riskEventRepository.findAdminRiskEvents(15002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "FREEZE_USER",
                                  "note": "temporary hold"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "UNFREEZE_USER",
                                  "note": "case cleared"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskStatus").value("HANDLED"))
                .andExpect(jsonPath("$.resultNote").value("case cleared"));

        UserDistributionProfile profile = userDistributionProfileRepository.findById(15002L).orElseThrow();
        DistributionRelation relation = distributionRelationRepository.findByUserId(15002L).orElseThrow();
        RewardRecord rewardRecord = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-unfreeze-admin-1").getFirst();

        org.assertj.core.api.Assertions.assertThat(profile.getUserStatus().name()).isEqualTo("NORMAL");
        org.assertj.core.api.Assertions.assertThat(relation.getLockStatus().name()).isEqualTo("UNLOCKED");
        org.assertj.core.api.Assertions.assertThat(rewardRecord.getRewardStatus().name()).isEqualTo("AVAILABLE");
        org.assertj.core.api.Assertions.assertThat(operationAuditLogRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldIgnoreRiskEventWithoutFreezingUser() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(16001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(16002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(16002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-ignore-1", 16002L, new BigDecimal("90.00"), "USD", LocalDateTime.now());
        Long riskEventId = riskEventRepository.findAdminRiskEvents(16002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "IGNORE",
                                  "note": "false positive"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskStatus").value("IGNORED"))
                .andExpect(jsonPath("$.resultNote").value("false positive"));
    }

    @Test
    void shouldReturnRecentAuditLogsAfterRiskActions() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(17001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(17002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(17002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-audit-1", 17002L, new BigDecimal("100.00"), "USD", LocalDateTime.now());
        Long riskEventId = riskEventRepository.findAdminRiskEvents(17002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();

        String adminSession = loginAsAdmin();
        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "FREEZE_USER",
                                  "note": "audit trail check"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/distribution/audit-logs")
                        .header("X-Admin-Session", adminSession)
                        .param("moduleName", "risk_event")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].moduleName").value("risk_event"))
                .andExpect(jsonPath("$.items[0].actionName").value("FREEZE_USER"))
                .andExpect(jsonPath("$.items[0].targetId").value(riskEventId))
                .andExpect(jsonPath("$.items[0].remark").value("audit trail check"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldManuallyAdjustRelationAndWriteAuditLog() throws Exception {
        String rootCode = distributionBindingService.createProfile(17101L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(17102L, "ID", "id", rootCode);
        String newRootCode = distributionBindingService.createProfile(17103L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(17104L, "ID", "id", newRootCode);
        distributionBindingService.createProfile(17105L, "ID", "id", rootCode);

        String adminSession = loginAsAdmin();
        mockMvc.perform(post("/admin/distribution/relation/17105/adjustments")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level1InviterId": 17104,
                                  "note": "manual correction after ops review"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(17105))
                .andExpect(jsonPath("$.level1InviterId").value(17104))
                .andExpect(jsonPath("$.level2InviterId").value(17103))
                .andExpect(jsonPath("$.level3InviterId").doesNotExist())
                .andExpect(jsonPath("$.bindSource").value("MANUAL"))
                .andExpect(jsonPath("$.lockStatus").value("UNLOCKED"));

        DistributionRelation relation = distributionRelationRepository.findByUserId(17105L).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(relation.getLevel1InviterId()).isEqualTo(17104L);
        org.assertj.core.api.Assertions.assertThat(relation.getLevel2InviterId()).isEqualTo(17103L);
        org.assertj.core.api.Assertions.assertThat(relation.getLevel3InviterId()).isNull();
        org.assertj.core.api.Assertions.assertThat(relation.getBindSource().name()).isEqualTo("MANUAL");
        org.assertj.core.api.Assertions.assertThat(operationAuditLogRepository.findAdminAuditLogs("relation", org.springframework.data.domain.PageRequest.of(0, 1))
                        .getContent().getFirst().getActionName())
                .isEqualTo("MANUAL_ADJUST");
    }

    @Test
    void shouldRestrictManualRelationAdjustmentByProductCode() throws Exception {
        String linkyRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+6281111111101", "17301")).record().getInviteCode();
        distributionBindingService.createProfile(17302L, "ID", "id", linkyRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+6281111111102", "17302"));
        distributionBindingService.createProfile(17303L, "ID", "id", linkyRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+6281111111103", "17303"));

        String toolRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+6282222222201", "17401")).record().getInviteCode();
        distributionBindingService.createProfile(17402L, "ID", "id", toolRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+6282222222202", "17402"));

        String adminSession = loginAsAdmin();
        mockMvc.perform(post("/admin/distribution/relation/17303/adjustments")
                        .header("X-Admin-Session", adminSession)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level1InviterId": 17302,
                                  "note": "linky correction"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(17303))
                .andExpect(jsonPath("$.level1InviterId").value(17302));

        mockMvc.perform(post("/admin/distribution/relation/17303/adjustments")
                        .header("X-Admin-Session", adminSession)
                        .param("product", "TOOLX")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level1InviterId": 17402,
                                  "note": "cross product should fail"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("distribution relation not found for product"));
    }

    @Test
    void shouldRejectManualAdjustForLockedRelation() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(17201L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(17202L, "ID", "id", rootCode);
        distributionBindingService.createProfile(17203L, "ID", "id", null);
        rewardCalculationService.processIncomeEvent("evt-locked-relation-1", 17202L, new BigDecimal("66.00"), "USD", LocalDateTime.now());

        mockMvc.perform(post("/admin/distribution/relation/17202/adjustments")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "level1InviterId": 17203,
                                  "note": "should fail for locked relation"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("locked relation cannot be adjusted manually"));
    }

    @Test
    void shouldRejectFreezeForIgnoredRiskEvent() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(18001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(18002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(18002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-invalid-freeze-1", 18002L, new BigDecimal("100.00"), "USD", LocalDateTime.now());
        Long riskEventId = riskEventRepository.findAdminRiskEvents(18002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();
        String adminSession = loginAsAdmin();

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "IGNORE",
                                  "note": "false positive"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "FREEZE_USER",
                                  "note": "should fail"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ignored risk event cannot freeze user"));
    }

    @Test
    void shouldRejectUnfreezeWhenRelationIsNotLocked() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(19001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(19002L, "ID", "id", rootCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(19002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);
        rewardCalculationService.processIncomeEvent("evt-invalid-unfreeze-1", 19002L, new BigDecimal("100.00"), "USD", LocalDateTime.now());
        Long riskEventId = riskEventRepository.findAdminRiskEvents(19002L, null, null, null, org.springframework.data.domain.PageRequest.of(0, 1))
                .getContent()
                .getFirst()
                .getId();

        mockMvc.perform(post("/admin/distribution/risk-events/" + riskEventId + "/actions")
                        .header("X-Admin-Session", loginAsAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "UNFREEZE_USER",
                                  "note": "should fail"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("user is not frozen"));
    }

    @Test
    void shouldCorrectOwnershipAndWriteAuditLog() throws Exception {
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+6283111111101", "19801"));
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+6283222222201", "19802"));

        String adminSession = loginAsAdmin();

        mockMvc.perform(get("/admin/distribution/ownership/19801")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(19801))
                .andExpect(jsonPath("$.items[0].productCode").value("LINKY"))
                .andExpect(jsonPath("$.items[0].ownershipStatus").value("ACTIVE"));

        mockMvc.perform(post("/admin/distribution/ownership/19801/corrections")
                        .header("X-Admin-Session", adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "TOOLX",
                                  "note": "ops corrected product ownership"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(19801))
                .andExpect(jsonPath("$.items[0].productCode").value("TOOLX"))
                .andExpect(jsonPath("$.items[0].ownershipStatus").value("ACTIVE"));

        org.assertj.core.api.Assertions.assertThat(userProductOwnershipRepository.findByUserIdAndProductCode(19801L, "LINKY").orElseThrow().getOwnershipStatus())
                .isEqualTo("CORRECTED");
        org.assertj.core.api.Assertions.assertThat(userProductOwnershipRepository.findByUserIdAndProductCode(19801L, "TOOLX").orElseThrow().getOwnershipStatus())
                .isEqualTo("ACTIVE");
        org.assertj.core.api.Assertions.assertThat(operationAuditLogRepository.findAdminAuditLogs("ownership", org.springframework.data.domain.PageRequest.of(0, 1))
                        .getContent().getFirst().getActionName())
                .isEqualTo("MANUAL_CORRECT");

        mockMvc.perform(get("/admin/distribution/audit-logs")
                        .header("X-Admin-Session", adminSession)
                        .param("moduleName", "ownership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].moduleName").value("ownership"))
                .andExpect(jsonPath("$.items[0].actionName").value("MANUAL_CORRECT"))
                .andExpect(jsonPath("$.items[0].remark").value("ops corrected product ownership"));
    }

    @Test
    void shouldReturnLinkyWebhookLogsForAdmin() throws Exception {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(19501L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(19502L, "ID", "id", inviterCode);

        String timestamp = java.time.Instant.now().toString();
        String signature = signLinkyRequest("linky-order-admin-1", 19502L, new BigDecimal("88.00"), "USD", "2026-04-21T10:00:00", timestamp);

        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", timestamp)
                        .header("X-Linky-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "linky-order-admin-1",
                                  "memberId": 19502,
                                  "commissionAmount": 88.00,
                                  "currency": "USD",
                                  "settledAt": "2026-04-21T10:00:00"
                                }
                                """))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(linkyWebhookLogRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/admin/distribution/linky-webhook-logs")
                        .header("X-Admin-Session", loginAsAdmin())
                        .param("linkyOrderId", "linky-order-admin-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].linkyOrderId").value("linky-order-admin-1"))
                .andExpect(jsonPath("$.items[0].userId").value(19502))
                .andExpect(jsonPath("$.items[0].signatureStatus").value("VALID"))
                .andExpect(jsonPath("$.items[0].replayStatus").value("VALID"))
                .andExpect(jsonPath("$.items[0].replayRecordStatus").value("FIRST_SEEN"))
                .andExpect(jsonPath("$.items[0].replayHitCount").value(1))
                .andExpect(jsonPath("$.items[0].requestStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/admin/distribution/linky-replay-records")
                        .header("X-Admin-Session", loginAsAdmin())
                        .param("linkyOrderId", "linky-order-admin-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].linkyOrderId").value("linky-order-admin-1"))
                .andExpect(jsonPath("$.items[0].userId").value(19502))
                .andExpect(jsonPath("$.items[0].hitCount").value(1))
                .andExpect(jsonPath("$.items[0].latestRequestStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldFilterLinkyWebhookLogsAndReplayRecordsByProductCode() throws Exception {
        seedRules();
        String linkyRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****3331", "19601")).record().getInviteCode();
        distributionBindingService.createProfile(19602L, "ID", "id", linkyRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****3332", "19602"));

        String toolRootCode = inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628****4441", "19701")).record().getInviteCode();
        distributionBindingService.createProfile(19702L, "ID", "id", toolRootCode);
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("TOOLX", "+628****4442", "19702"));

        String linkyTimestamp = java.time.Instant.now().toString();
        String linkySignature = signLinkyRequest("linky-order-product-1", 19602L, new BigDecimal("66.00"), "USD", "2026-04-21T12:00:00", linkyTimestamp);
        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", linkyTimestamp)
                        .header("X-Linky-Signature", linkySignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "linky-order-product-1",
                                  "memberId": 19602,
                                  "commissionAmount": 66.00,
                                  "currency": "USD",
                                  "settledAt": "2026-04-21T12:00:00"
                                }
                                """))
                .andExpect(status().isOk());

        String toolTimestamp = java.time.Instant.now().plusSeconds(1).toString();
        String toolSignature = signLinkyRequest("toolx-order-product-1", 19702L, new BigDecimal("77.00"), "USD", "2026-04-21T13:00:00", toolTimestamp);
        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", toolTimestamp)
                        .header("X-Linky-Signature", toolSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "toolx-order-product-1",
                                  "memberId": 19702,
                                  "commissionAmount": 77.00,
                                  "currency": "USD",
                                  "settledAt": "2026-04-21T13:00:00"
                                }
                                """))
                .andExpect(status().isOk());

        String adminSessionToken = loginAsAdmin();

        mockMvc.perform(get("/admin/distribution/linky-webhook-logs")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].linkyOrderId").value("linky-order-product-1"))
                .andExpect(jsonPath("$.items[0].userId").value(19602));

        mockMvc.perform(get("/admin/distribution/linky-replay-records")
                        .header("X-Admin-Session", adminSessionToken)
                        .param("product", "LINKY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].linkyOrderId").value("linky-order-product-1"))
                .andExpect(jsonPath("$.items[0].userId").value(19602));
    }

    private String loginAsAdmin() throws Exception {
        String response = mockMvc.perform(post("/admin/auth/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "test-admin-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.replaceAll(".*\\\"sessionToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private String signLinkyRequest(String linkyOrderId, Long userId, BigDecimal incomeAmount, String currencyCode, String paidAt, String timestamp) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec("test-linky-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = timestamp + "." + linkyOrderId.trim() + "." + userId + "." + incomeAmount.toPlainString() + "." + currencyCode.trim().toUpperCase() + "." + paidAt;
            byte[] signed = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void seedRules() {
        LocalDateTime effectiveFrom = LocalDateTime.now().minusYears(1);
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L, effectiveFrom, null));
    }
}
