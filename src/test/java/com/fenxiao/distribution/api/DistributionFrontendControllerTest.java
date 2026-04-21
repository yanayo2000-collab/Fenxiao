package com.fenxiao.distribution.api;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.reward.service.RewardCalculationService;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@SpringBootTest
class DistributionFrontendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Test
    void shouldReturnDistributionHomeSummary() throws Exception {
        seedRules();
        String rootCode = distributionBindingService.createProfile(20001L, "ID", "id", null).getInviteCode();
        UserDistributionProfile leaderProfile = distributionBindingService.createProfile(20002L, "ID", "id", rootCode);
        String leaderCode = leaderProfile.getInviteCode();
        distributionBindingService.createProfile(20003L, "ID", "id", leaderCode);
        distributionBindingService.createProfile(20004L, "ID", "id", leaderCode);

        rewardCalculationService.processIncomeEvent("evt-home-1", 20003L, new BigDecimal("100.00"), "USD", LocalDateTime.now().minusDays(10));
        rewardCalculationService.processIncomeEvent("evt-home-2", 20004L, new BigDecimal("40.00"), "USD", LocalDateTime.now());
        rewardCalculationService.unlockDueRewards(LocalDateTime.now());

        mockMvc.perform(get("/api/distribution/home/20002")
                        .header("X-Distribution-Token", leaderProfile.getApiAccessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(20002))
                .andExpect(jsonPath("$.invitedUsers").value(2))
                .andExpect(jsonPath("$.effectiveUsers").value(2))
                .andExpect(jsonPath("$.availableReward").value(15.0))
                .andExpect(jsonPath("$.frozenReward").value(6.0))
                .andExpect(jsonPath("$.totalReward").value(21.0));
    }

    @Test
    void shouldReturnDirectTeamMembers() throws Exception {
        UserDistributionProfile rootProfile = distributionBindingService.createProfile(21001L, "ID", "id", null);
        String rootCode = rootProfile.getInviteCode();
        distributionBindingService.createProfile(21002L, "ID", "id", rootCode);
        distributionBindingService.createProfile(21003L, "ID", "id", rootCode);

        mockMvc.perform(get("/api/distribution/team/21001")
                        .header("X-Distribution-Token", rootProfile.getApiAccessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].userId").exists())
                .andExpect(jsonPath("$.items[0].lockStatus").value("UNLOCKED"));
    }

    @Test
    void shouldForbidAccessWhenHeaderUserDoesNotMatchPathUser() throws Exception {
        UserDistributionProfile rootProfile = distributionBindingService.createProfile(21501L, "ID", "id", null);
        String rootCode = rootProfile.getInviteCode();
        distributionBindingService.createProfile(21502L, "ID", "id", rootCode);

        mockMvc.perform(get("/api/distribution/team/21501")
                        .header("X-Distribution-Token", "forged-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnRewardDetailsForBeneficiary() throws Exception {
        seedRules();
        UserDistributionProfile rootProfile = distributionBindingService.createProfile(22001L, "ID", "id", null);
        String rootCode = rootProfile.getInviteCode();
        distributionBindingService.createProfile(22002L, "ID", "id", rootCode);

        rewardCalculationService.processIncomeEvent("evt-reward-detail-1", 22002L, new BigDecimal("50.00"), "USD", LocalDateTime.now());

        mockMvc.perform(get("/api/distribution/rewards/22001")
                        .header("X-Distribution-Token", rootProfile.getApiAccessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].beneficiaryUserId").value(22001))
                .andExpect(jsonPath("$.items[0].rewardStatus").value("FROZEN"));
    }

    private void seedRules() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0);
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L, effectiveFrom, null));
    }
}
