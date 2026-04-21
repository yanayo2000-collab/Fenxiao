package com.fenxiao.admin.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = "app.admin.token=test-admin-token")
class DistributionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
