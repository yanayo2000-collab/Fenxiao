package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.service.DistributionQueryService;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import com.fenxiao.relationship.entity.InvitationRelationVersion;
import com.fenxiao.relationship.repository.InvitationRelationVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RelationAdjustmentService {

    private static final String MODULE_NAME = "relation";
    private static final String TARGET_TYPE = "distribution_relation";

    private final DistributionRelationRepository distributionRelationRepository;
    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final OperationAuditLogRepository operationAuditLogRepository;
    private final AdminProductScopeService adminProductScopeService;
    private final InvitationRelationVersionRepository invitationRelationVersionRepository;
    private final Clock clock;

    @Autowired
    public RelationAdjustmentService(DistributionRelationRepository distributionRelationRepository,
                                     UserDistributionProfileRepository userDistributionProfileRepository,
                                     OperationAuditLogRepository operationAuditLogRepository,
                                     AdminProductScopeService adminProductScopeService,
                                     InvitationRelationVersionRepository invitationRelationVersionRepository) {
        this(distributionRelationRepository, userDistributionProfileRepository, operationAuditLogRepository, adminProductScopeService, invitationRelationVersionRepository, Clock.systemUTC());
    }

    RelationAdjustmentService(DistributionRelationRepository distributionRelationRepository,
                              UserDistributionProfileRepository userDistributionProfileRepository,
                              OperationAuditLogRepository operationAuditLogRepository,
                              AdminProductScopeService adminProductScopeService,
                              InvitationRelationVersionRepository invitationRelationVersionRepository,
                              Clock clock) {
        this.distributionRelationRepository = distributionRelationRepository;
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.operationAuditLogRepository = operationAuditLogRepository;
        this.adminProductScopeService = adminProductScopeService;
        this.invitationRelationVersionRepository = invitationRelationVersionRepository;
        this.clock = clock;
    }

    public RelationDetailResponse adjustRelation(Long userId,
                                                 Long level1InviterId,
                                                 String note,
                                                 String requestIp,
                                                 String productCode,
                                                 Long operatorId,
                                                 String operatorRole) {
        String normalizedProductCode = adminProductScopeService.normalizeProductCode(productCode);
        if (normalizedProductCode != null) {
            List<Long> scopedUserIds = adminProductScopeService.resolveScopedUserIds(normalizedProductCode);
            if (!scopedUserIds.contains(userId)) {
                throw new IllegalArgumentException("distribution relation not found for product");
            }
            if (level1InviterId != null && !scopedUserIds.contains(level1InviterId)) {
                throw new IllegalArgumentException("inviter relation not found for product");
            }
        }

        DistributionRelation relation = distributionRelationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));
        UserDistributionProfile profile = userDistributionProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));

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
        InvitationRelationVersion currentVersion = invitationRelationVersionRepository.findTopByUserIdOrderByVersionNoDesc(userId)
                .orElseThrow(() -> new IllegalStateException("invitation relation version not found"));
        if (currentVersion.getEffectiveTo() == null) currentVersion.endAt(now);
        invitationRelationVersionRepository.save(InvitationRelationVersion.create(
                userId, level1InviterId, currentVersion.getVersionNo() + 1, now,
                normalizedNote == null || normalizedNote.isBlank() ? "SUPER_ADMIN_CORRECTION" : normalizedNote,
                "ADMIN_CORRECTION", null, operatorId));
        operationAuditLogRepository.save(OperationAuditLog.create(
                operatorId,
                operatorRole,
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
