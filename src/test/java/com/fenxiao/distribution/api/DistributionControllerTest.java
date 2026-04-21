package com.fenxiao.distribution.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@SpringBootTest
class DistributionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateProfileAndReturnInviteCode() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", 0,
                                "countryCode", "",
                                "languageCode", ""
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestForUnknownInviteCode() throws Exception {
        mockMvc.perform(post("/api/distribution/profiles")
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
}
