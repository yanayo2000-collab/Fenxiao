package com.fenxiao.admin.api;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.service.LinkyGuildProbeClient;
import com.fenxiao.distribution.service.LinkyGuildProbeResult;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.admin.token=test-admin-token",
        "app.distribution.profile-create-token=test-create-token",
        "app.distribution.internal-token=test-token"
})
class DistributionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @MockBean
    private LinkyGuildProbeClient linkyGuildProbeClient;

    @Test
    void shouldReturnForbiddenWithoutAdminCredentials() throws Exception {
        mockMvc.perform(get("/admin/distribution/rewards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldCreateAdminSessionWithValidLogin() throws Exception {
        mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "test-admin-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").isString())
                .andExpect(jsonPath("$.expiresAt").isString());
    }

    @Test
    void shouldRejectAdminSessionLoginWithInvalidPassword() throws Exception {
        mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.2");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "wrong-token"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRateLimitRepeatedAdminLoginFailures() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/admin/auth/session")
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.3");
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "password": "wrong-token"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.3");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "wrong-token"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldRejectLegacyAdminTokenOnProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Token", "test-admin-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectInvalidRewardPaginationParameters() throws Exception {
        String response = mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.5");
                            return request;
                        })
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

        String sessionToken = response.replaceAll(".*\"sessionToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Session", sessionToken)
                        .param("page", "-1")
                        .param("size", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnRewardListEndpointPayloadWithSessionToken() throws Exception {
        String response = mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.4");
                            return request;
                        })
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

        String sessionToken = response.replaceAll(".*\"sessionToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/admin/distribution/rewards")
                        .header("X-Admin-Session", sessionToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void shouldRefreshLinkyEligibilityFromGuildProbe() throws Exception {
        when(linkyGuildProbeClient.probe("12345678"))
                .thenReturn(LinkyGuildProbeResult.matchedOurs("12345678", "413", "Permata", "probe matched"));

        String response = mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.6");
                            return request;
                        })
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

        String sessionToken = response.replaceAll(".*\"sessionToken\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/admin/distribution/linky-eligibility-checks/12345678/refresh")
                        .header("X-Admin-Session", sessionToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkyAccount").value("12345678"))
                .andExpect(jsonPath("$.guildId").value("413"))
                .andExpect(jsonPath("$.guildName").value("Permata"))
                .andExpect(jsonPath("$.guildCheckStatus").value("MATCHED_OURS"))
                .andExpect(jsonPath("$.registrationEligibility").value("ELIGIBLE"));
    }

    @Test
    void shouldApproveWithdrawRequestFromAdminEndpoint() throws Exception {
        String sessionToken = createAdminSessionToken("10.0.0.7");

        UserDistributionProfile profile = distributionBindingService.createProfile(62001L, "ID", "id", null);
        String userToken = profile.getApiAccessToken();
        rewardRecordRepository.save(makeAvailableReward("income-62001-a", profile.getUserId(), 1, "1200.000000"));

        String withdrawResponse = mockMvc.perform(post("/api/distribution/withdraw-requests/62001")
                        .header("X-Distribution-Token", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestNo = withdrawResponse.replaceAll(".*\"requestNo\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/admin/distribution/withdraw-requests/" + requestNo + "/approve")
                        .header("X-Admin-Session", sessionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "manual payout done"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNo").value(requestNo))
                .andExpect(jsonPath("$.requestStatus").value("PAID_OUT"));
    }

    @Test
    void shouldRejectWithdrawRequestFromAdminEndpoint() throws Exception {
        String sessionToken = createAdminSessionToken("10.0.0.8");

        UserDistributionProfile profile = distributionBindingService.createProfile(62002L, "ID", "id", null);
        String userToken = profile.getApiAccessToken();
        rewardRecordRepository.save(makeAvailableReward("income-62002-a", profile.getUserId(), 1, "1200.000000"));

        String withdrawResponse = mockMvc.perform(post("/api/distribution/withdraw-requests/62002")
                        .header("X-Distribution-Token", userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String requestNo = withdrawResponse.replaceAll(".*\"requestNo\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/admin/distribution/withdraw-requests/" + requestNo + "/reject")
                        .header("X-Admin-Session", sessionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "bank account mismatch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNo").value(requestNo))
                .andExpect(jsonPath("$.requestStatus").value("REJECTED"));
    }

    private String createAdminSessionToken(String remoteAddr) throws Exception {
        String response = mockMvc.perform(post("/admin/auth/session")
                        .with(request -> {
                            request.setRemoteAddr(remoteAddr);
                            return request;
                        })
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

    private RewardRecord makeAvailableReward(String sourceEventId, Long beneficiaryUserId, int rewardLevel, String rewardAmount) {
        RewardRecord record = RewardRecord.create(
                sourceEventId,
                beneficiaryUserId,
                88000L + rewardLevel,
                rewardLevel,
                new BigDecimal(rewardAmount),
                new BigDecimal("0.100000"),
                new BigDecimal(rewardAmount),
                "DIAMOND",
                0,
                LocalDateTime.of(2026, 5, 9, 0, 0)
        );
        record.markAvailable();
        return record;
    }
}
