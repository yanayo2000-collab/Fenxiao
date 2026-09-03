package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.AdminLoginRequest;
import com.fenxiao.admin.api.dto.AdminSessionResponse;
import com.fenxiao.admin.api.dto.ChangeAdminPasswordRequest;
import com.fenxiao.admin.service.AdminAccountManagementService;
import com.fenxiao.admin.service.AdminSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminSessionService adminSessionService;
    private final AdminAccountManagementService accountManagementService;

    public AdminAuthController(AdminSessionService adminSessionService, AdminAccountManagementService accountManagementService) {
        this.adminSessionService = adminSessionService;
        this.accountManagementService = accountManagementService;
    }

    @PostMapping("/session")
    public AdminSessionResponse createSession(@Valid @RequestBody AdminLoginRequest request,
                                              HttpServletRequest httpServletRequest,
                                              HttpServletResponse response) {
        AdminSessionResponse session = adminSessionService.createSession(request.username(), request.password(), request.remembersDevice(),
                httpServletRequest.getRemoteAddr(), httpServletRequest.getHeader("User-Agent"));
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(session.sessionToken(), request.remembersDevice()).toString());
        return session;
    }

    @GetMapping("/session")
    public AdminSessionResponse current(@CookieValue(name = "bandeira_admin_session", required = false) String cookie,
                                        HttpServletRequest request) {
        return adminSessionService.current(resolve(request.getHeader("X-Admin-Session"), cookie));
    }

    @PostMapping("/session/logout")
    public void logout(@CookieValue(name = "bandeira_admin_session", required = false) String cookie,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        adminSessionService.revokeCurrent(resolve(request.getHeader("X-Admin-Session"), cookie), "USER_LOGOUT");
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie().toString());
    }

    @PostMapping("/password")
    public void changePassword(@RequestHeader(value="X-Admin-Session",required=false) String header,
                               @CookieValue(name="bandeira_admin_session",required=false) String cookie,
                               @Valid @RequestBody ChangeAdminPasswordRequest request,
                               HttpServletRequest http,HttpServletResponse response){
        String token=resolve(header,cookie); var principal=adminSessionService.assertSession(token);
        accountManagementService.changeOwnPassword(principal,request.currentPassword(),request.newPassword(),http.getRemoteAddr());
        response.addHeader(HttpHeaders.SET_COOKIE,clearCookie().toString());
    }

    @PostMapping("/session/logout-all")
    public void logoutAll(@RequestHeader(value="X-Admin-Session",required=false) String header,
                          @CookieValue(name="bandeira_admin_session",required=false) String cookie,
                          HttpServletResponse response){
        var principal=adminSessionService.assertSession(resolve(header,cookie));
        adminSessionService.revokeAll(principal.accountId(),"USER_LOGOUT_ALL");
        response.addHeader(HttpHeaders.SET_COOKIE,clearCookie().toString());
    }

    static ResponseCookie sessionCookie(String token, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("bandeira_admin_session", token)
                .httpOnly(true).secure(true).sameSite("Lax").path("/admin");
        if (rememberMe) builder.maxAge(java.time.Duration.ofDays(400));
        return builder.build();
    }

    static ResponseCookie clearCookie() {
        return ResponseCookie.from("bandeira_admin_session", "").httpOnly(true).secure(true).sameSite("Lax")
                .path("/admin").maxAge(java.time.Duration.ZERO).build();
    }

    private String resolve(String header, String cookie) { return header == null || header.isBlank() ? cookie : header; }
}
