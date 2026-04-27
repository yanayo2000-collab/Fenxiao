package com.fenxiao.distribution.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenxiao.distribution.service.DistributionBindingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

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

    @Test
    void shouldRejectProfileCreationWithoutCreateToken() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5009,
                                "countryCode", "ID",
                                "languageCode", "id"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldCreateProfileAndReturnInviteCode() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5001,
                                "countryCode", "ID",
                                "languageCode", "id"
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
                                "languageCode", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateProfileCreation() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5003,
                                "countryCode", "ID",
                                "languageCode", "id"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/distribution/profiles")
                        .header("X-Profile-Create-Token", "test-create-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 5003,
                                "countryCode", "ID",
                                "languageCode", "id"
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

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode,
                                "whatsappNumber", "+6281234500002",
                                "linkyAccount", "87654321"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/distribution/bindings/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "inviteCode", inviteCode,
                                "whatsappNumber", "+6281234500002",
                                "linkyAccount", "12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("whatsapp number already registered"));
    }

    @Test
    void shouldIssueInviteCodeFromProductWhatsappAndAppAccount() throws Exception {
        mockMvc.perform(post("/api/distribution/invite-codes/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "productCode", "linky",
                                "whatsappNumber", "+6281234567890",
                                "appAccount", "12345678"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(12345678))
                .andExpect(jsonPath("$.productCode").value("LINKY"))
                .andExpect(jsonPath("$.whatsappNumber").value("+6281234567890"))
                .andExpect(jsonPath("$.appAccount").value("12345678"))
                .andExpect(jsonPath("$.inviteCode").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
