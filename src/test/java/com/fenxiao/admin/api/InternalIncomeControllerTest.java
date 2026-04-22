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
@SpringBootTest(properties = {
        "app.distribution.internal-token=test-token",
        "app.distribution.linky-signing-secret=test-linky-secret"
})
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

    @Test
    void shouldAcceptLinkyIncomeEventAndMapToInternalFlow() throws Exception {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(24101L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(24102L, "ID", "id", inviterCode);

        Map<String, Object> request = Map.of(
                "linkyOrderId", "linky-order-1",
                "userId", 24102,
                "incomeAmount", new BigDecimal("120.50"),
                "currencyCode", "USD",
                "paidAt", "2026-04-21T13:30:00"
        );

        String timestamp = "2026-04-22T04:00:00Z";
        String signature = signLinkyRequest("linky-order-1", 24102L, new BigDecimal("120.50"), "USD", "2026-04-21T13:30:00", timestamp);

        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", timestamp)
                        .header("X-Linky-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceEventId").value("LINKY:linky-order-1"))
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", timestamp)
                        .header("X-Linky-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceEventId").value("LINKY:linky-order-1"))
                .andExpect(jsonPath("$.status").value("DUPLICATE"));
    }

    @Test
    void shouldValidateLinkyPayload() throws Exception {
        Map<String, Object> request = Map.of(
                "linkyOrderId", "",
                "userId", 24102,
                "incomeAmount", new BigDecimal("88.00"),
                "currencyCode", "USD",
                "paidAt", "2026-04-21T10:00:00"
        );

        String timestamp = "2026-04-22T04:00:00Z";
        String signature = signLinkyRequest("", 24102L, new BigDecimal("88.00"), "USD", "2026-04-21T10:00:00", timestamp);

        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", timestamp)
                        .header("X-Linky-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectLinkyIncomeEventWithInvalidSignature() throws Exception {
        Map<String, Object> request = Map.of(
                "linkyOrderId", "linky-order-2",
                "userId", 24102,
                "incomeAmount", new BigDecimal("88.00"),
                "currencyCode", "USD",
                "paidAt", "2026-04-21T10:00:00"
        );

        mockMvc.perform(post("/internal/distribution/linky/income-events")
                        .header("X-Internal-Token", "test-token")
                        .header("X-Linky-Timestamp", "2026-04-22T04:00:00Z")
                        .header("X-Linky-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
        LocalDateTime effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0);
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L, effectiveFrom, null));
    }
}
