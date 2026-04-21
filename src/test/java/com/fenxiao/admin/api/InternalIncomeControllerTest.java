package com.fenxiao.admin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@SpringBootTest(properties = "app.distribution.internal-token=test-token")
class InternalIncomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Test
    void shouldAcceptInternalIncomeEventAndReturnProcessedStatus() throws Exception {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(24001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(24002L, "ID", "id", inviterCode);

        Map<String, Object> request = Map.of(
                "sourceEventId", "evt-internal-1",
                "userId", 24002,
                "incomeAmount", new BigDecimal("88.00"),
                "currencyCode", "USD",
                "eventTime", "2026-04-21T10:00:00"
        );

        mockMvc.perform(post("/internal/distribution/income-events")
                        .header("X-Internal-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceEventId").value("evt-internal-1"))
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        mockMvc.perform(post("/internal/distribution/income-events")
                        .header("X-Internal-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DUPLICATE"));
    }

    @Test
    void shouldRejectInternalIncomeEventWithoutValidToken() throws Exception {
        Map<String, Object> request = Map.of(
                "sourceEventId", "evt-internal-2",
                "userId", 24002,
                "incomeAmount", new BigDecimal("88.00"),
                "currencyCode", "USD",
                "eventTime", "2026-04-21T10:00:00"
        );

        mockMvc.perform(post("/internal/distribution/income-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void seedRules() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0);
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L, effectiveFrom, null));
    }
}
