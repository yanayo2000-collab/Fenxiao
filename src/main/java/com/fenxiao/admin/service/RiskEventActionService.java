package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RiskEventAction;
import com.fenxiao.admin.api.dto.RiskEventListItem;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.risk.entity.RiskEvent;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Transactional
public class RiskEventActionService {

    private static final long SYSTEM_ADMIN_OPERATOR_ID = 0L;
    private static final String OPERATOR_ROLE = "ADMIN_SESSION";
    private static final String MODULE_NAME = "risk_event";
    private static final String TARGET_TYPE = "risk_event";

    private final RiskEventRepository riskEventRepository;
    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final DistributionRelationRepository distributionRelationRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final OperationAuditLogRepository operationAuditLogRepository;
    private final Clock clock;

    @Autowired
    public RiskEventActionService(RiskEventRepository riskEventRepository,
                                  UserDistributionProfileRepository userDistributionProfileRepository,
                                  DistributionRelationRepository distributionRelationRepository,
                                  RewardRecordRepository rewardRecordRepository,
                                  OperationAuditLogRepository operationAuditLogRepository) {
        this(riskEventRepository,
                userDistributionProfileRepository,
                distributionRelationRepository,
                rewardRecordRepository,
                operationAuditLogRepository,
                Clock.systemUTC());
    }

    RiskEventActionService(RiskEventRepository riskEventRepository,
                           UserDistributionProfileRepository userDistributionProfileRepository,
                           DistributionRelationRepository distributionRelationRepository,
                           RewardRecordRepository rewardRecordRepository,
                           OperationAuditLogRepository operationAuditLogRepository,
                           Clock clock) {
        this.riskEventRepository = riskEventRepository;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.distributionRelationRepository = distributionRelationRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.operationAuditLogRepository = operationAuditLogRepository;
        this.clock = clock;
    }

    public RiskEventListItem applyAction(Long riskEventId, RiskEventAction action, String note, String requestIp) {
        RiskEvent riskEvent = riskEventRepository.findById(riskEventId)
                .orElseThrow(() -> new IllegalArgumentException("risk event not found"));
        UserDistributionProfile profile = userDistributionProfileRepository.findById(riskEvent.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));
        DistributionRelation relation = distributionRelationRepository.findByUserId(riskEvent.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));

        validateAction(riskEvent, profile, action);

        String beforeSnapshot = snapshot(riskEvent, profile, relation);
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedNote = note == null ? null : note.trim();

        switch (action) {
            case HANDLE -> riskEvent.markHandled(SYSTEM_ADMIN_OPERATOR_ID, now, normalizedNote);
            case IGNORE -> riskEvent.markIgnored(SYSTEM_ADMIN_OPERATOR_ID, now, normalizedNote);
            case FREEZE_USER -> {
                profile.markAsRiskUser();
                relation.lock(now);
                moveRewardsToRiskHold(profile.getUserId(), normalizedNote == null || normalizedNote.isBlank() ? "risk event frozen by admin" : normalizedNote);
                riskEvent.markHandled(SYSTEM_ADMIN_OPERATOR_ID, now, normalizedNote);
            }
            case UNFREEZE_USER -> {
                profile.markAsNormalUser();
                relation.unlock();
                restoreRewardsFromRiskHold(profile.getUserId(), now);
                riskEvent.markHandled(SYSTEM_ADMIN_OPERATOR_ID, now, normalizedNote);
            }
        }

        riskEventRepository.save(riskEvent);
        userDistributionProfileRepository.save(profile);
        distributionRelationRepository.save(relation);

        operationAuditLogRepository.save(OperationAuditLog.create(
                SYSTEM_ADMIN_OPERATOR_ID,
                OPERATOR_ROLE,
                MODULE_NAME,
                TARGET_TYPE,
                riskEvent.getId(),
                action.name(),
                beforeSnapshot,
                snapshot(riskEvent, profile, relation),
                requestIp,
                normalizedNote,
                now
        ));

        return RiskEventQueryService.toItem(riskEvent);
    }

    private void moveRewardsToRiskHold(Long userId, String reason) {
        LinkedHashMap<Long, RewardRecord> rewardRecordMap = new LinkedHashMap<>();
        for (RewardRecord rewardRecord : rewardRecordRepository.findByBeneficiaryUserIdAndRewardStatusIn(
                userId,
                List.of(RewardStatus.FROZEN, RewardStatus.AVAILABLE, RewardStatus.RISK_HOLD)
        )) {
            rewardRecordMap.put(rewardRecord.getId(), rewardRecord);
        }
        for (RewardRecord rewardRecord : rewardRecordRepository.findBySourceUserIdAndRewardStatusIn(
                userId,
                List.of(RewardStatus.FROZEN, RewardStatus.AVAILABLE, RewardStatus.RISK_HOLD)
        )) {
            rewardRecordMap.put(rewardRecord.getId(), rewardRecord);
        }
        for (RewardRecord rewardRecord : rewardRecordMap.values()) {
            rewardRecord.markRiskHold(reason);
        }
        rewardRecordRepository.saveAll(rewardRecordMap.values());
    }

    private void restoreRewardsFromRiskHold(Long userId, LocalDateTime now) {
        LinkedHashMap<Long, RewardRecord> rewardRecordMap = new LinkedHashMap<>();
        for (RewardRecord rewardRecord : rewardRecordRepository.findByBeneficiaryUserIdAndRewardStatus(userId, RewardStatus.RISK_HOLD)) {
            rewardRecordMap.put(rewardRecord.getId(), rewardRecord);
        }
        for (RewardRecord rewardRecord : rewardRecordRepository.findBySourceUserIdAndRewardStatus(userId, RewardStatus.RISK_HOLD)) {
            rewardRecordMap.put(rewardRecord.getId(), rewardRecord);
        }
        for (RewardRecord rewardRecord : rewardRecordMap.values()) {
            rewardRecord.releaseFromRiskHold(now);
        }
        rewardRecordRepository.saveAll(rewardRecordMap.values());
    }

    private String snapshot(RiskEvent riskEvent, UserDistributionProfile profile, DistributionRelation relation) {
        return "riskStatus=" + riskEvent.getRiskStatus()
                + ",handledBy=" + riskEvent.getHandledBy()
                + ",handledAt=" + riskEvent.getHandledAt()
                + ",userStatus=" + profile.getUserStatus()
                + ",lockStatus=" + relation.getLockStatus()
                + ",lockTime=" + relation.getLockTime();
    }

    private void validateAction(RiskEvent riskEvent, UserDistributionProfile profile, RiskEventAction action) {
        switch (action) {
            case HANDLE -> {
                if (riskEvent.getRiskStatus() != com.fenxiao.risk.domain.RiskStatus.PENDING) {
                    throw new IllegalArgumentException("only pending risk event can be handled");
                }
            }
            case IGNORE -> {
                if (riskEvent.getRiskStatus() != com.fenxiao.risk.domain.RiskStatus.PENDING) {
                    throw new IllegalArgumentException("only pending risk event can be ignored");
                }
            }
            case FREEZE_USER -> {
                if (riskEvent.getRiskStatus() == com.fenxiao.risk.domain.RiskStatus.IGNORED) {
                    throw new IllegalArgumentException("ignored risk event cannot freeze user");
                }
                if (riskEvent.getRiskStatus() != com.fenxiao.risk.domain.RiskStatus.PENDING) {
                    throw new IllegalArgumentException("only pending risk event can freeze user");
                }
            }
            case UNFREEZE_USER -> {
                if (riskEvent.getRiskStatus() != com.fenxiao.risk.domain.RiskStatus.HANDLED || !hasRiskHoldExposure(profile.getUserId())) {
                    throw new IllegalArgumentException("user is not frozen");
                }
            }
        }
    }

    private boolean hasRiskHoldExposure(Long userId) {
        return !rewardRecordRepository.findByBeneficiaryUserIdAndRewardStatus(userId, RewardStatus.RISK_HOLD).isEmpty()
                || !rewardRecordRepository.findBySourceUserIdAndRewardStatus(userId, RewardStatus.RISK_HOLD).isEmpty();
    }
}
