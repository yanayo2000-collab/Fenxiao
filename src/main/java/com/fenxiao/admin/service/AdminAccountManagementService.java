package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.*;
import com.fenxiao.admin.entity.AdminAccount;
import com.fenxiao.admin.entity.AdminPasswordHistory;
import com.fenxiao.admin.repository.AdminAccountRepository;
import com.fenxiao.admin.repository.AdminPasswordHistoryRepository;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.common.api.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
public class AdminAccountManagementService {
    private static final Set<String> ROLES=Set.of("super_admin","admin","operator","operations","mentor","team_leader","finance","customer_support");
    private static final SecureRandom RANDOM=new SecureRandom();
    private final AdminAccountRepository accounts; private final AdminPasswordHistoryRepository history;
    private final AdminPasswordHasher hasher; private final AdminPasswordPolicy policy; private final AdminSessionService sessions;
    private final OperationAuditLogRepository audits; private final AdminSecurityEventService securityEvents; private final long passwordMaxAgeDays;

    public AdminAccountManagementService(AdminAccountRepository accounts,AdminPasswordHistoryRepository history,AdminPasswordHasher hasher,
                                         AdminPasswordPolicy policy,AdminSessionService sessions,OperationAuditLogRepository audits,AdminSecurityEventService securityEvents,
                                         @Value("${app.admin.password-max-age-days:90}") long passwordMaxAgeDays){
        this.accounts=accounts;this.history=history;this.hasher=hasher;this.policy=policy;this.sessions=sessions;this.audits=audits;this.securityEvents=securityEvents;this.passwordMaxAgeDays=passwordMaxAgeDays;
    }

    @Transactional(readOnly=true)
    public List<AdminAccountResponse> list(){return accounts.findAll().stream().map(this::response).toList();}

    @Transactional
    public AdminAccountCreatedResponse create(CreateAdminAccountRequest request,AdminSessionService.AdminPrincipal actor,String ip){
        String username=normalizeUsername(request.username()); if(accounts.findByUsername(username).isPresent())throw new IllegalArgumentException("admin username already exists");
        String role=validateRole(request.role()); String temporaryPassword=temporaryPassword(); policy.validate(username,temporaryPassword);
        AdminAccount account=AdminAccount.create(username,request.displayName(),role,hasher.hash(temporaryPassword),true);
        account.updateProfile(request.displayName(),role,request.platformScope(),request.guildScope(),request.regionScope());
        account.updatePasswordHash(account.getPasswordHash(),true,LocalDateTime.now()); accounts.save(account);
        audit(actor,"CREATE_ACCOUNT",account,null,snapshot(account),ip,"temporary password issued once");
        securityEvents.record(account.getId(),account.getUsername(),"ACCOUNT_CREATED",true,ip,null,"created by "+actor.username());
        return new AdminAccountCreatedResponse(response(account),temporaryPassword);
    }

    @Transactional
    public AdminAccountResponse update(long id,UpdateAdminAccountRequest request,AdminSessionService.AdminPrincipal actor,String ip){
        AdminAccount account=require(id); String before=snapshot(account); String nextRole=validateRole(request.role());
        boolean removingLastSuper=account.isEnabled()&&"super_admin".equals(account.getRole())&&(!request.enabled()||!"super_admin".equals(nextRole));
        if(removingLastSuper&&accounts.countByRoleIgnoreCaseAndEnabledTrue("super_admin")<=1)throw new IllegalArgumentException("cannot disable or demote the last super admin");
        if(account.getId()==actor.accountId()&&!request.enabled())throw new IllegalArgumentException("cannot disable current account");
        boolean securityChanged=!account.getRole().equals(nextRole)||account.isEnabled()!=request.enabled();
        account.updateProfile(request.displayName(),nextRole,request.platformScope(),request.guildScope(),request.regionScope()); account.setEnabled(request.enabled());
        if(securityChanged)sessions.revokeAll(account.getId(),"ACCOUNT_SECURITY_CHANGED");
        audit(actor,"UPDATE_ACCOUNT",account,before,snapshot(account),ip,null);
        securityEvents.record(account.getId(),account.getUsername(),"ACCOUNT_UPDATED",true,ip,null,"updated by "+actor.username()); return response(account);
    }

    @Transactional
    public void changeOwnPassword(AdminSessionService.AdminPrincipal actor,String currentPassword,String newPassword,String ip){
        AdminAccount account=require(actor.accountId()); if(!hasher.matches(currentPassword,account.getPasswordHash()))throw new ForbiddenException("current password invalid");
        applyPassword(account,newPassword,false); sessions.revokeAll(account.getId(),"PASSWORD_CHANGED");
        audit(actor,"CHANGE_PASSWORD",account,null,"{\"sessionsRevoked\":true}",ip,null);
    }

    @Transactional
    public AdminAccountCreatedResponse resetPassword(long id,AdminSessionService.AdminPrincipal actor,String ip){
        AdminAccount account=require(id); String temporaryPassword=temporaryPassword(); applyPassword(account,temporaryPassword,true);
        sessions.revokeAll(account.getId(),"PASSWORD_RESET"); audit(actor,"RESET_PASSWORD",account,null,"{\"mustChangePassword\":true}",ip,"temporary password issued once");
        securityEvents.record(account.getId(),account.getUsername(),"PASSWORD_RESET",true,ip,null,"reset by "+actor.username());
        return new AdminAccountCreatedResponse(response(account),temporaryPassword);
    }

    @Transactional
    public AdminAccountResponse unlock(long id,AdminSessionService.AdminPrincipal actor,String ip){
        AdminAccount account=require(id); account.clearLoginFailures();
        audit(actor,"UNLOCK_ACCOUNT",account,null,"{\"lockedUntil\":null}",ip,null);
        securityEvents.record(account.getId(),account.getUsername(),"ACCOUNT_UNLOCKED",true,ip,null,"unlocked by "+actor.username());
        return response(account);
    }

    @Transactional(readOnly=true)
    public List<AdminDeviceSessionResponse> deviceSessions(AdminSessionService.AdminPrincipal actor){
        return sessions.activeSessions(actor.accountId()).stream().map(s->new AdminDeviceSessionResponse(s.getId(),s.getId()==actor.sessionId(),s.isRememberMe(),s.getIssuedAt(),s.getLastSeenAt(),s.getExpiresAt(),s.getIpAddress(),s.getUserAgent())).toList();
    }

    @Transactional
    public void revokeDevice(AdminSessionService.AdminPrincipal actor,long sessionId,String ip){
        var target=sessions.activeSessions(actor.accountId()).stream().filter(s->s.getId()==sessionId).findFirst().orElseThrow(()->new IllegalArgumentException("session not found"));
        target.revoke(LocalDateTime.now(),"DEVICE_REVOKED"); audit(actor,"REVOKE_SESSION",require(actor.accountId()),null,"{\"sessionId\":"+sessionId+"}",ip,null);
    }

    private void applyPassword(AdminAccount account,String password,boolean mustChange){
        policy.validate(account.getUsername(),password);
        if(hasher.matches(password,account.getPasswordHash())||history.findTop5ByAccountIdOrderByCreatedAtDesc(account.getId()).stream().anyMatch(h->hasher.matches(password,h.getPasswordHash())))
            throw new IllegalArgumentException("password was used recently");
        LocalDateTime now=LocalDateTime.now(); history.save(AdminPasswordHistory.create(account.getId(),account.getPasswordHash(),now));
        account.updatePasswordHash(hasher.hash(password),mustChange,now);
    }

    private AdminAccount require(long id){return accounts.findById(id).orElseThrow(()->new IllegalArgumentException("admin account not found"));}
    private String normalizeUsername(String v){return v.trim().toLowerCase();}
    private String validateRole(String v){String role=v==null?"":v.trim().toLowerCase();if(!ROLES.contains(role))throw new IllegalArgumentException("unsupported admin role");return role;}
    private String temporaryPassword(){byte[] b=new byte[15];RANDOM.nextBytes(b);return "B!"+Base64.getUrlEncoder().withoutPadding().encodeToString(b)+"9a";}
    private AdminAccountResponse response(AdminAccount a){LocalDateTime expires=a.getPasswordChangedAt()==null?null:a.getPasswordChangedAt().plusDays(passwordMaxAgeDays);return new AdminAccountResponse(a.getId(),a.getUsername(),a.getDisplayName(),a.getRole(),a.isEnabled(),a.getPlatformScope(),a.getGuildScope(),a.getRegionScope(),a.isMustChangePassword(),a.getLastLoginAt(),a.getPasswordChangedAt(),expires,a.getLockedUntil(),sessions.activeSessions(a.getId()).size());}
    private String snapshot(AdminAccount a){return "{\"username\":\""+a.getUsername()+"\",\"role\":\""+a.getRole()+"\",\"enabled\":"+a.isEnabled()+"}";}
    private void audit(AdminSessionService.AdminPrincipal actor,String action,AdminAccount target,String before,String after,String ip,String remark){audits.save(OperationAuditLog.create(actor.accountId(),actor.role(),"admin_account","admin_account",target.getId(),action,before,after,ip,remark,LocalDateTime.now()));}
}
