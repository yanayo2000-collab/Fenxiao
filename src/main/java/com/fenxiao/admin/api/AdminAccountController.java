package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.*;
import com.fenxiao.admin.service.AdminAccountManagementService;
import com.fenxiao.admin.service.AdminPermission;
import com.fenxiao.admin.service.AdminSessionService;
import com.fenxiao.admin.service.AdminSecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/accounts")
public class AdminAccountController {
    private final AdminSessionService sessions; private final AdminAccountManagementService accounts; private final AdminSecurityEventService securityEvents;
    public AdminAccountController(AdminSessionService sessions,AdminAccountManagementService accounts,AdminSecurityEventService securityEvents){this.sessions=sessions;this.accounts=accounts;this.securityEvents=securityEvents;}

    @GetMapping
    public List<AdminAccountResponse> list(@RequestHeader("X-Admin-Session") String token){sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE);return accounts.list();}

    @PostMapping
    public AdminAccountCreatedResponse create(@RequestHeader("X-Admin-Session") String token,@Valid @RequestBody CreateAdminAccountRequest request,HttpServletRequest http){
        return accounts.create(request,sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE),http.getRemoteAddr());
    }

    @PatchMapping("/{id}")
    public AdminAccountResponse update(@RequestHeader("X-Admin-Session") String token,@PathVariable long id,@Valid @RequestBody UpdateAdminAccountRequest request,HttpServletRequest http){
        return accounts.update(id,request,sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE),http.getRemoteAddr());
    }

    @PostMapping("/{id}/reset-password")
    public AdminAccountCreatedResponse reset(@RequestHeader("X-Admin-Session") String token,@PathVariable long id,HttpServletRequest http){
        return accounts.resetPassword(id,sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE),http.getRemoteAddr());
    }

    @PostMapping("/{id}/unlock")
    public AdminAccountResponse unlock(@RequestHeader("X-Admin-Session") String token,@PathVariable long id,HttpServletRequest http){
        return accounts.unlock(id,sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE),http.getRemoteAddr());
    }

    @GetMapping("/me/sessions")
    public List<AdminDeviceSessionResponse> deviceSessions(@RequestHeader("X-Admin-Session") String token){return accounts.deviceSessions(sessions.assertSession(token));}

    @DeleteMapping("/me/sessions/{sessionId}")
    public Map<String,Boolean> revokeDevice(@RequestHeader("X-Admin-Session") String token,@PathVariable long sessionId,HttpServletRequest http){
        accounts.revokeDevice(sessions.assertSession(token),sessionId,http.getRemoteAddr());return Map.of("revoked",true);
    }

    @GetMapping("/me/security-events")
    public List<AdminSecurityEventResponse> myEvents(@RequestHeader("X-Admin-Session") String token){return securityEvents.recent(sessions.assertSession(token).accountId());}

    @GetMapping("/{id}/security-events")
    public List<AdminSecurityEventResponse> accountEvents(@RequestHeader("X-Admin-Session") String token,@PathVariable long id){sessions.assertPermission(token,AdminPermission.ACCOUNT_MANAGE);return securityEvents.recent(id);}
}
