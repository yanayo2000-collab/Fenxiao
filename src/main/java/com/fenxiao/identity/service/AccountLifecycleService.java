package com.fenxiao.identity.service;

import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.identity.domain.AccountStatus;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class AccountLifecycleService {
    private final UserDistributionProfileRepository profileRepository;
    private final UserSessionService sessionService;
    private final OperationAuditLogRepository auditRepository;
    private final Clock clock;

    public AccountLifecycleService(UserDistributionProfileRepository profileRepository,
                                   UserSessionService sessionService,
                                   OperationAuditLogRepository auditRepository,
                                   Clock clock) {
        this.profileRepository = profileRepository;
        this.sessionService = sessionService;
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    public UserDistributionProfile change(Long userId, String action, String reason, Long operatorId, String operatorRole, String requestIp) {
        UserDistributionProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));
        AccountStatus before = profile.getAccountStatus();
        String normalizedAction = action == null ? "" : action.trim().toUpperCase();
        switch (normalizedAction) {
            case "FREEZE" -> profile.freezeAccount();
            case "ACTIVATE" -> profile.activateAccount();
            case "CANCEL" -> profile.cancelAccount(LocalDateTime.now(clock));
            default -> throw new IllegalArgumentException("unsupported account action");
        }
        sessionService.revokeAll(userId);
        profileRepository.save(profile);
        auditRepository.save(OperationAuditLog.create(operatorId, operatorRole, "account", "user", userId,
                normalizedAction, "{\"accountStatus\":\"" + before + "\"}",
                "{\"accountStatus\":\"" + profile.getAccountStatus() + "\"}", requestIp, reason, LocalDateTime.now(clock)));
        return profile;
    }
}
