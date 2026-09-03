package com.fenxiao.admin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenxiao.admin.entity.AdminAccount;
import com.fenxiao.admin.repository.AdminAccountRepository;
import com.fenxiao.admin.service.AdminPasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminIdentityManagementTest {
    @Autowired MockMvc mvc; @Autowired AdminAccountRepository accounts; @Autowired AdminPasswordHasher hasher; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;

    @BeforeEach void seed(){accounts.deleteAll();accounts.save(AdminAccount.create("root_admin","Root Admin","super_admin",hasher.hash("Root-Secure-Password-2026!"),true));}

    @Test void managesEmployeeResetAndForcedPasswordChange() throws Exception {
        String root=login("root_admin","Root-Secure-Password-2026!",true);
        String created=mvc.perform(post("/admin/accounts").header("X-Admin-Session",root).contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"finance_one","displayName":"Finance One","role":"finance","platformScope":"LINKY","guildScope":"GUILD-A","regionScope":"BR"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.account.mustChangePassword").value(true)).andReturn().getResponse().getContentAsString();
        String temporary=json.readTree(created).path("temporaryPassword").asText(); assertThat(temporary).hasSizeGreaterThanOrEqualTo(12);
        String employee=login("finance_one",temporary,true);
        mvc.perform(get("/admin/distribution/reports/overview").header("X-Admin-Session",employee)).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("admin password change required"));
        mvc.perform(post("/admin/auth/password").header("X-Admin-Session",employee).contentType(MediaType.APPLICATION_JSON).content("""
                {"currentPassword":"%s","newPassword":"Finance-New-Password-2026!"}
                """.formatted(temporary))).andExpect(status().isOk());
        mvc.perform(get("/admin/accounts/me/sessions").header("X-Admin-Session",employee)).andExpect(status().isForbidden());
        String renewed=login("finance_one","Finance-New-Password-2026!",false);
        mvc.perform(get("/admin/distribution/reports/overview").param("product","LINKY").header("X-Admin-Session",renewed)).andExpect(status().isOk());
        mvc.perform(get("/admin/distribution/reports/overview").param("product","TIMO").header("X-Admin-Session",renewed)).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("admin data scope denied"));
    }

    @Test void protectsLastSuperAdminAndRevokesLogoutServerSide() throws Exception {
        String root=login("root_admin","Root-Secure-Password-2026!",true);
        long id=accounts.findByUsername("root_admin").orElseThrow().getId();
        mvc.perform(patch("/admin/accounts/{id}",id).header("X-Admin-Session",root).contentType(MediaType.APPLICATION_JSON).content("""
                {"displayName":"Root Admin","role":"admin","enabled":true,"platformScope":"*","guildScope":"*","regionScope":"*"}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("cannot disable or demote the last super admin"));
        mvc.perform(post("/admin/auth/session/logout").header("X-Admin-Session",root)).andExpect(status().isOk());
        mvc.perform(get("/admin/accounts").header("X-Admin-Session",root)).andExpect(status().isForbidden());
    }

    @Test void remembersDeviceWithPersistentSecureCookie() throws Exception {
        var result=mvc.perform(post("/admin/auth/session").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"root_admin","password":"Root-Secure-Password-2026!","rememberMe":true}
                """)).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("bandeira_admin_session="),org.hamcrest.Matchers.containsString("HttpOnly"),org.hamcrest.Matchers.containsString("Secure"),org.hamcrest.Matchers.containsString("Max-Age=")))).andReturn();
        JsonNode body=json.readTree(result.getResponse().getContentAsString()); assertThat(body.path("rememberMe").asBoolean()).isTrue();
    }

    @Test void rememberedCookieAuthenticatesAllAdminEndpointsAndLogoutRevokesIt() throws Exception {
        String token=login("root_admin","Root-Secure-Password-2026!",true);
        Cookie cookie=new Cookie("bandeira_admin_session",token);
        mvc.perform(get("/admin/accounts").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].role").value("super_admin"));
        mvc.perform(post("/admin/auth/session/logout").cookie(cookie)).andExpect(status().isOk());
        mvc.perform(get("/admin/accounts").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test void legacyMfaFlagDoesNotRequireASecondFactorAndMfaEndpointsAreRemoved() throws Exception {
        AdminAccount root=accounts.findByUsername("root_admin").orElseThrow();
        jdbc.update("update admin_account set mfa_enabled=true,mfa_secret='legacy-disabled-secret' where id=?",root.getId());
        String token=login("root_admin","Root-Secure-Password-2026!",true);
        mvc.perform(post("/admin/accounts/me/mfa/setup").header("X-Admin-Session",token))
                .andExpect(status().isNotFound());
    }

    @Test void superAdminCanUnlockEmployeeAndActionIsAudited() throws Exception {
        String root=login("root_admin","Root-Secure-Password-2026!",true);
        AdminAccount employee=AdminAccount.create("locked_employee","Locked Employee","operator",hasher.hash("Locked-Employee-Password-2026!"),true);
        employee.recordFailedLogin(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),1,30); employee=accounts.save(employee);
        mvc.perform(post("/admin/accounts/{id}/unlock",employee.getId()).header("X-Admin-Session",root))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lockedUntil").doesNotExist());
        assertThat(accounts.findById(employee.getId()).orElseThrow().getLockedUntil()).isNull();
        Integer events=jdbc.queryForObject("select count(*) from admin_security_event where account_id=? and event_type='ACCOUNT_UNLOCKED'",Integer.class,employee.getId());
        assertThat(events).isEqualTo(1);
    }

    @Test void slidesRememberedSessionAndExpiresAfterSevenIdleDays() throws Exception {
        String token=login("root_admin","Root-Secure-Password-2026!",true);
        jdbc.update("update admin_session set last_seen_at=dateadd('DAY',-1,current_timestamp),expires_at=dateadd('DAY',6,current_timestamp) where token_hash=?",sha(token));
        mvc.perform(get("/admin/auth/session").header("X-Admin-Session",token)).andExpect(status().isOk());
        java.sql.Timestamp renewed=jdbc.queryForObject("select expires_at from admin_session where token_hash=?",java.sql.Timestamp.class,sha(token));
        assertThat(renewed.toLocalDateTime()).isAfter(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(6).plusHours(23));
        jdbc.update("update admin_session set expires_at=? where token_hash=?",java.sql.Timestamp.valueOf(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(1)),sha(token));
        mvc.perform(get("/admin/auth/session").header("X-Admin-Session",token)).andExpect(status().isForbidden());
    }

    private String sha(String value) throws Exception {return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}

    private String login(String username,String password,boolean remember) throws Exception {
        String body=mvc.perform(post("/admin/auth/session").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"%s","password":"%s","rememberMe":%s}
                """.formatted(username,password,remember))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("sessionToken").asText();
    }
}
