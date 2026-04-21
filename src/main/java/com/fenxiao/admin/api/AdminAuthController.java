package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.AdminLoginRequest;
import com.fenxiao.admin.api.dto.AdminSessionResponse;
import com.fenxiao.admin.service.AdminSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminSessionService adminSessionService;

    public AdminAuthController(AdminSessionService adminSessionService) {
        this.adminSessionService = adminSessionService;
    }

    @PostMapping("/session")
    public AdminSessionResponse createSession(@Valid @RequestBody AdminLoginRequest request,
                                              HttpServletRequest httpServletRequest) {
        return adminSessionService.createSession(request.password(), httpServletRequest.getRemoteAddr());
    }
}
