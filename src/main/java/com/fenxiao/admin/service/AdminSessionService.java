package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.AdminSessionResponse;
import com.fenxiao.admin.entity.AdminAccount;
import com.fenxiao.admin.entity.AdminSession;
import com.fenxiao.admin.repository.AdminAccountRepository;
import com.fenxiao.admin.repository.AdminSessionRepository;
import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.common.api.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final long sessionTtlMinutes;
    private final long rememberedIdleDays;
    private final long loginWindowMinutes;
    private final int loginMaxAttempts;
    private final long accountLockMinutes;
    private final long passwordMaxAgeDays;
    private final Clock clock;
    private final AdminAccountRepository accountRepository;
    private final AdminSessionRepository sessionRepository;
    private final AdminPasswordHasher passwordHasher;
    private final AdminSecurityEventService securityEvents;
    private final ConcurrentHashMap<String, FailedLoginWindow> clientFailures = new ConcurrentHashMap<>();

    @Autowired
    public AdminSessionService(@Value("${app.admin.session-ttl-minutes:720}") long sessionTtlMinutes,
                               @Value("${app.admin.remembered-idle-days:7}") long rememberedIdleDays,
                               @Value("${app.admin.login-window-minutes:10}") long loginWindowMinutes,
                               @Value("${app.admin.login-max-attempts:5}") int loginMaxAttempts,
                               @Value("${app.admin.account-lock-minutes:30}") long accountLockMinutes,
                               @Value("${app.admin.password-max-age-days:90}") long passwordMaxAgeDays,
                               AdminAccountRepository accountRepository,
                               AdminSessionRepository sessionRepository,
                               AdminPasswordHasher passwordHasher,
                               AdminSecurityEventService securityEvents) {
        this(sessionTtlMinutes, rememberedIdleDays, loginWindowMinutes, loginMaxAttempts, accountLockMinutes,passwordMaxAgeDays,
                Clock.systemUTC(), accountRepository, sessionRepository, passwordHasher, securityEvents);
    }

    AdminSessionService(long sessionTtlMinutes, long rememberedIdleDays, long loginWindowMinutes,
                        int loginMaxAttempts, long accountLockMinutes,long passwordMaxAgeDays, Clock clock,
                        AdminAccountRepository accountRepository, AdminSessionRepository sessionRepository,
                        AdminPasswordHasher passwordHasher, AdminSecurityEventService securityEvents) {
        this.sessionTtlMinutes=sessionTtlMinutes; this.rememberedIdleDays=rememberedIdleDays;
        this.loginWindowMinutes=loginWindowMinutes; this.loginMaxAttempts=loginMaxAttempts;
        this.accountLockMinutes=accountLockMinutes; this.clock=clock; this.accountRepository=accountRepository;
        this.passwordMaxAgeDays=passwordMaxAgeDays;
        this.sessionRepository=sessionRepository; this.passwordHasher=passwordHasher; this.securityEvents=securityEvents;
    }

    @Transactional
    public AdminSessionResponse createSession(String username,String password,boolean rememberMe,String clientKey,String userAgent){
        LocalDateTime now=now(); String normalizedClientKey=normalizeClientKey(clientKey); enforceClientRateLimit(normalizedClientKey);
        String normalizedUsername=normalizeUsername(username);
        AdminAccount account=accountRepository.findByUsername(normalizedUsername).filter(AdminAccount::isEnabled).orElse(null);
        if(account!=null && account.isLockedAt(now)) throw new TooManyRequestsException("admin account temporarily locked");
        if(account==null || !passwordHasher.matches(password,account.getPasswordHash())){
            recordFailure(normalizedClientKey,account,now); securityEvents.record(account==null?null:account.getId(),normalizedUsername,"LOGIN",false,clientKey,userAgent,"INVALID_CREDENTIALS"); throw new ForbiddenException("admin login invalid");
        }
        boolean newIp=!securityEvents.knownIp(account.getId(),clientKey);
        clientFailures.remove(normalizedClientKey); account.clearLoginFailures(); account.recordLogin(now);
        String token=newToken(); LocalDateTime expiresAt=rememberMe?now.plusDays(rememberedIdleDays):now.plusMinutes(sessionTtlMinutes);
        sessionRepository.save(AdminSession.issue(account.getId(),hashToken(token),rememberMe,now,expiresAt,clientKey,truncate(userAgent,512)));
        securityEvents.record(account.getId(),account.getUsername(),newIp?"NEW_DEVICE_LOGIN":"LOGIN",true,clientKey,userAgent,newIp?"new network address":null);
        return response(token,expiresAt,account,rememberMe);
    }

    @Transactional
    public AdminPrincipal assertSession(String token){
        if(token==null||token.isBlank()) throw new ForbiddenException("admin session invalid");
        LocalDateTime now=now(); AdminSession session=sessionRepository.findByTokenHash(hashToken(token))
                .filter(value->value.isUsableAt(now)).orElseThrow(()->new ForbiddenException("admin session invalid or expired"));
        AdminAccount account=accountRepository.findById(session.getAccountId()).filter(AdminAccount::isEnabled)
                .orElseThrow(()->new ForbiddenException("admin session invalid"));
        if(session.getLastSeenAt().isBefore(now.minusMinutes(15))) session.touch(now,rememberedIdleDays);
        return principal(account,session.getId(),session.isRememberMe(),session.getExpiresAt());
    }

    @Transactional
    public AdminSessionResponse current(String token){
        AdminPrincipal p=assertSession(token);
        AdminAccount account=accountRepository.findById(p.accountId()).orElseThrow();
        return response("",p.expiresAt(),account,p.rememberMe());
    }

    public AdminPrincipal assertPermission(String token,AdminPermission permission){
        AdminPrincipal principal=assertSession(token);
        if(principal.mustChangePassword()) throw new ForbiddenException("admin password change required");
        if(!permission.allows(principal.role())) throw new ForbiddenException("admin permission denied");
        return principal;
    }

    @Transactional
    public void revokeCurrent(String token,String reason){
        if(token==null||token.isBlank()) return;
        sessionRepository.findByTokenHash(hashToken(token)).ifPresent(s->s.revoke(now(),reason));
    }

    @Transactional
    public int revokeAll(long accountId,String reason){
        LocalDateTime at=now(); List<AdminSession> sessions=sessionRepository.findByAccountIdAndRevokedAtIsNull(accountId);
        sessions.forEach(s->s.revoke(at,reason)); return sessions.size();
    }

    @Transactional(readOnly=true)
    public List<AdminSession> activeSessions(long accountId){
        LocalDateTime at=now(); return sessionRepository.findByAccountIdAndRevokedAtIsNull(accountId).stream().filter(s->s.isUsableAt(at)).toList();
    }

    private void recordFailure(String clientKey,AdminAccount account,LocalDateTime now){
        clientFailures.compute(clientKey,(k,v)->v==null||v.windowStart().plusMinutes(loginWindowMinutes).isBefore(now)
                ?new FailedLoginWindow(now,1):new FailedLoginWindow(v.windowStart(),v.failedAttempts()+1));
        if(account!=null) account.recordFailedLogin(now,loginMaxAttempts,accountLockMinutes);
    }

    private void enforceClientRateLimit(String key){
        FailedLoginWindow v=clientFailures.get(key); if(v==null)return; LocalDateTime at=now();
        if(v.windowStart().plusMinutes(loginWindowMinutes).isBefore(at)){clientFailures.remove(key);return;}
        if(v.failedAttempts()>=loginMaxAttempts) throw new TooManyRequestsException("admin login rate limited");
    }

    private AdminSessionResponse response(String token,LocalDateTime expiresAt,AdminAccount a,boolean remembered){
        String passwordExpiresAt=a.getPasswordChangedAt()==null?null:a.getPasswordChangedAt().plusDays(passwordMaxAgeDays).toInstant(ZoneOffset.UTC).toString();
        return new AdminSessionResponse(token,expiresAt.toInstant(ZoneOffset.UTC).toString(),a.getUsername(),a.getDisplayName(),a.getRole(),a.isMustChangePassword(),remembered,passwordExpiresAt,a.getPlatformScope(),a.getGuildScope(),a.getRegionScope());
    }
    private AdminPrincipal principal(AdminAccount a,long sessionId,boolean remembered,LocalDateTime expiresAt){
        return new AdminPrincipal(a.getId(),a.getUsername(),a.getDisplayName(),a.getRole(),a.isMustChangePassword(),sessionId,remembered,expiresAt,a.getPlatformScope(),a.getGuildScope(),a.getRegionScope());
    }
    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    private String newToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return TOKEN_ENCODER.encodeToString(bytes);}
    public String hashToken(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String normalizeClientKey(String v){return v==null||v.isBlank()?"unknown":v;}
    private String normalizeUsername(String v){if(v==null||v.isBlank())throw new ForbiddenException("admin login invalid");return v.trim().toLowerCase();}
    private String truncate(String value,int max){return value==null?null:value.substring(0,Math.min(max,value.length()));}
    private record FailedLoginWindow(LocalDateTime windowStart,int failedAttempts){}
    public record AdminPrincipal(long accountId,String username,String displayName,String role,boolean mustChangePassword,long sessionId,boolean rememberMe,LocalDateTime expiresAt,String platformScope,String guildScope,String regionScope){
        public void requireScope(String platform,String guild,String region){
            if(!allows(platformScope,platform)||!allows(guildScope,guild)||!allows(regionScope,region))throw new ForbiddenException("admin data scope denied");
        }
        private boolean allows(String scope,String value){if(value==null||value.isBlank()||scope==null||scope.isBlank()||"*".equals(scope))return true;for(String item:scope.split(","))if(item.trim().equalsIgnoreCase(value.trim()))return true;return false;}
    }
}
