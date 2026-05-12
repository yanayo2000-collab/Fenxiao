package com.fenxiao.distribution.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.service.LinkyGuildProbeClient;
import com.fenxiao.distribution.service.LinkyGuildProbeResult;
import com.fenxiao.distribution.service.LinkyRegistrationEligibilityService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = "app.distribution.profile-create-token=test-create-token")
class DistributionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private LinkyRegistrationEligibilityService linkyRegistrationEligibilityService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @MockBean
    private LinkyGuildProbeClient linkyGuildProbeClient;

    @Test
    void shouldRejectProfileCreationWithoutCreateToken() throws Exception {
        String initialInviteCode = distributionBindingService.createProfile(59000L, "ID", "id", null).getInviteCode();

        mockMvc.perform(post("/api/distribution/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5009,
                                "countryCode", "ID",
                                "languageCode", "id",
                                "inviteCode", initialInviteCode
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectProfileCreationWithoutInviteCode() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5001,
                                "countryCode", "ID",
                                "languageCode", "id"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateProfileUsingInitialInviteCode() throws Exception {
        String initialInviteCode = distributionBindingService.createProfile(59001L, "ID", "id", null).getInviteCode();

        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5001,
                                "countryCode", "ID",
                                "languageCode", "id",
                                "inviteCode", initialInviteCode
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5001))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.countryCode").value("ID"));
    }

    @Test
    void shouldRejectInvalidProfileRequest() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 0,
                                "countryCode", "",
                                "languageCode", "",
                                "inviteCode", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateProfileCreation() throws Exception {
        String initialInviteCode = distributionBindingService.createProfile(59002L, "ID", "id", null).getInviteCode();

        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5003,
                                "countryCode", "ID",
                                "languageCode", "id",
                                "inviteCode", initialInviteCode
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5003,
                                "countryCode", "ID",
                                "languageCode", "id",
                                "inviteCode", initialInviteCode
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("user profile already exists"));
    }

    @Test
    void shouldReturnBadRequestForUnknownInviteCode() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5002,
                                "countryCode", "ID",
                                "languageCode", "id",
                                "inviteCode", "unknown01"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invite code not found"));
    }

    @Test
    void shouldRegisterInviteBindingUsingInviteCodeWhatsappAndLinkyAccount() throws Exception {
        String inviteCode = distributionBindingService.createProfile(53001L, "ID", "id", null).getInviteCode();
        linkyRegistrationEligibilityService.markEligible("12345678", "GUILD-001", "Our Linky Guild", 9001L, "prechecked");

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode.toLowerCase(),
                                "whatsappNumber", "+6281234567890",
                                "linkyAccount", "12345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("LINKY"))
                .andExpect(jsonPath("$.inviteCode").value(inviteCode))
                .andExpect(jsonPath("$.inviterUserId").value(53001))
                .andExpect(jsonPath("$.whatsappNumber").value("+6281234567890"))
                .andExpect(jsonPath("$.linkyAccount").value("12345678"))
                .andExpect(jsonPath("$.bindStatus").value("ACTIVE"));
    }

    @Test
    void shouldRejectDuplicateBindingRegistration() throws Exception {
        String inviteCode = distributionBindingService.createProfile(53002L, "ID", "id", null).getInviteCode();
        linkyRegistrationEligibilityService.markEligible("87654321", "GUILD-001", "Our Linky Guild", 9001L, "prechecked");
        linkyRegistrationEligibilityService.markEligible("12345678", "GUILD-001", "Our Linky Guild", 9001L, "prechecked");

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode,
                                "whatsappNumber", "+628123450002",
                                "linkyAccount", "87654321"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode,
                                "whatsappNumber", "+628123450002",
                                "linkyAccount", "12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("whatsapp number already registered"));
    }

    @Test
    void shouldRejectBindingRegistrationWhenLinkyAccountIsOutsideOurGuild() throws Exception {
        String inviteCode = distributionBindingService.createProfile(53003L, "ID", "id", null).getInviteCode();
        when(linkyGuildProbeClient.probe("23456789"))
                .thenReturn(LinkyGuildProbeResult.notMatched("23456789", "probe empty result"));

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode,
                                "whatsappNumber", "+628123450003",
                                "linkyAccount", "23456789"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("linky account is not eligible for registration"));
    }

    @Test
    void shouldIssueInviteCodeFromProductWhatsappAndAppAccount() throws Exception {
        mockMvc.perform(post("/api/distribution/invite-codes/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "whatsappNumber", "+6281234567800",
                                "appAccount", "12345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(12345678))
                .andExpect(jsonPath("$.productCode").value("LINKY"))
                .andExpect(jsonPath("$.whatsappNumber").value("+6281234567800"))
                .andExpect(jsonPath("$.appAccount").value("12345678"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldCreateWithdrawRequestFromAvailableRewards() throws Exception {
        UserDistributionProfile profile = distributionBindingService.createProfile(54001L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-54001-a", profile.getUserId(), 1, "600.000000"));
        rewardRecordRepository.save(makeAvailableReward("evt-54001-b", profile.getUserId(), 2, "450.000000"));

        mockMvc.perform(post("/api/distribution/withdraw-requests/54001")
                        .header("X-Distribution-Token", profile.getApiAccessToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(54001))
                .andExpect(jsonPath("$.requestStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.requestedDiamondAmount").value(1050.000000));
    }

    private RewardRecord makeAvailableReward(String sourceEventId, Long beneficiaryUserId, int rewardLevel, String rewardAmount) {
        RewardRecord record = RewardRecord.create(
                sourceEventId,
                beneficiaryUserId,
                82000L + rewardLevel,
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
