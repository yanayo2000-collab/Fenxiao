package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class RelationAdjustmentService {

    private static final long SYSTEM_ADMIN_OPERATOR_ID = 0L;
    private static final String OPERATOR_ROLE = "ADMIN_SESSION";
    private static final String MODULE_NAME = "relation";
    private static final String TARGET_TYPE = "distribution_relation";

    private final DistributionRelationRepository distributionRelationRepository;
    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final OperationAuditLogRepository operationAuditLogRepository;
    private final Clock clock;

    @Autowired
    public RelationAdjustmentService(DistributionRelationRepository distributionRelationRepository,
                                     UserDistributionProfileRepository userDistributionProfileRepository,
                                     OperationAuditLogRepository operationAuditLogRepository) {
        this(distributionRelationRepository, userDistributionProfileRepository, operationAuditLogRepository, Clock.systemUTC());
    }

    RelationAdjustmentService(DistributionRelationRepository distributionRelationRepository,
                              UserDistributionProfileRepository userDistributionProfileRepository,
                              OperationAuditLogRepository operationAuditLogRepository,
                              Clock clock) {
        this.distributionRelationRepository = distributionRelationRepository;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.operationAuditLogRepository = operationAuditLogRepository;
        this.clock = clock;
    }

    public RelationDetailResponse adjustRelation(Long userId, Long level1InviterId, String note, String requestIp) {
        DistributionRelation relation = distributionRelationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));
        UserDistributionProfile profile = userDistributionProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));

        if (relation.getLockStatus() == com.fenxiao.distribution.domain.LockStatus.LOCKED) {
            throw new IllegalArgumentException("locked relation cannot be adjusted manually");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedNote = note == null ? null : note.trim();
        String beforeSnapshot = snapshot(relation);

        if (level1InviterId == null) {
            relation.rebindManually(null, null, null, false, now);
        } else {
            if (userId.equals(level1InviterId)) {
                throw new IllegalArgumentException("user cannot bind to self");
            }
            DistributionRelation inviterRelation = distributionRelationRepository.findByUserId(level1InviterId)
                    .orElseThrow(() -> new IllegalArgumentException("inviter relation not found"));
            if (isCycle(userId, inviterRelation)) {
                throw new IllegalArgumentException("manual relation adjustment would create cycle");
            }
            UserDistributionProfile inviterProfile = userDistributionProfileRepository.findById(level1InviterId)
                    .orElseThrow(() -> new IllegalArgumentException("inviter profile not found"));
            boolean crossCountry = !profile.getCountryCode().equalsIgnoreCase(inviterProfile.getCountryCode());
            relation.rebindManually(
                    inviterRelation.getUserId(),
                    inviterRelation.getLevel1InviterId(),
                    inviterRelation.getLevel2InviterId(),
                    crossCountry,
                    now
            );
        }

        distributionRelationRepository.save(relation);
        operationAuditLogRepository.save(OperationAuditLog.create(
                SYSTEM_ADMIN_OPERATOR_ID,
                OPERATOR_ROLE,
                MODULE_NAME,
                TARGET_TYPE,
                relation.getId(),
                "MANUAL_ADJUST",
                beforeSnapshot,
                snapshot(relation),
                requestIp,
                normalizedNote,
                now
        ));
        return DistributionQueryService.toDetailResponse(relation);
    }

    private boolean isCycle(Long userId, DistributionRelation inviterRelation) {
        return userId.equals(inviterRelation.getUserId())
                || userId.equals(inviterRelation.getLevel1InviterId())
                || userId.equals(inviterRelation.getLevel2InviterId())
                || userId.equals(inviterRelation.getLevel3InviterId());
    }

    private String snapshot(DistributionRelation relation) {
        return "userId=" + relation.getUserId()
                + ",level1InviterId=" + relation.getLevel1InviterId()
                + ",level2InviterId=" + relation.getLevel2InviterId()
                + ",level3InviterId=" + relation.getLevel3InviterId()
                + ",bindSource=" + relation.getBindSource()
                + ",bindTime=" + relation.getBindTime()
                + ",lockStatus=" + relation.getLockStatus();
    }
}
