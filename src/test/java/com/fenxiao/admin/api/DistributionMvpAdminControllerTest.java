package com.fenxiao.admin.api;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.reward.service.RewardCalculationService;
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
@SpringBootTest(properties = "app.admin.token=test-admin-token")
class DistributionMvpAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Autowired
    private UserDistributionProfileRepository userDistributionProfileRepository;

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

        return response.replaceAll(".*\"sessionToken\":\"([^\"]+)\".*", "$1");
    }

    private void seedRules() {
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L));
    }
}
