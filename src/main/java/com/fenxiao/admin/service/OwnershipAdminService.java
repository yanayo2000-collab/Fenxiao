package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.OwnershipDetailResponse;
import com.fenxiao.admin.api.dto.OwnershipItemResponse;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.distribution.entity.UserProductOwnership;
import com.fenxiao.distribution.repository.UserProductOwnershipRepository;
import com.fenxiao.distribution.service.UserProductOwnershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class OwnershipAdminService {

    private static final long SYSTEM_ADMIN_OPERATOR_ID = 0L;
    private static final String OPERATOR_ROLE = "ADMIN_SESSION";
    private static final String MODULE_NAME = "ownership";
    private static final String TARGET_TYPE = "user_product_ownership";

    private final UserProductOwnershipRepository userProductOwnershipRepository;
    private final UserProductOwnershipService userProductOwnershipService;
    private final OperationAuditLogRepository operationAuditLogRepository;
    private final Clock clock;

    @Autowired
    public OwnershipAdminService(UserProductOwnershipRepository userProductOwnershipRepository,
                                 UserProductOwnershipService userProductOwnershipService,
                                 OperationAuditLogRepository operationAuditLogRepository) {
        this(userProductOwnershipRepository, userProductOwnershipService, operationAuditLogRepository, Clock.systemUTC());
    }

    OwnershipAdminService(UserProductOwnershipRepository userProductOwnershipRepository,
                          UserProductOwnershipService userProductOwnershipService,
                          OperationAuditLogRepository operationAuditLogRepository,
                          Clock clock) {
        this.userProductOwnershipRepository = userProductOwnershipRepository;
        this.userProductOwnershipService = userProductOwnershipService;
        this.operationAuditLogRepository = operationAuditLogRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OwnershipDetailResponse getOwnership(Long userId) {
        List<UserProductOwnership> ownerships = userProductOwnershipRepository.findByUserIdOrderByIdDesc(userId);
        if (ownerships.isEmpty()) {
            throw new IllegalArgumentException("ownership not found");
        }
        return toResponse(userId, ownerships);
    }

    public OwnershipDetailResponse correctOwnership(Long userId, String productCode, String note, String requestIp) {
        List<UserProductOwnership> ownerships = userProductOwnershipRepository.findByUserIdOrderByIdDesc(userId);
        if (ownerships.isEmpty()) {
            throw new IllegalArgumentException("ownership not found");
        }
        String normalizedProductCode = productCode.trim().toUpperCase(Locale.ROOT);
        String beforeSnapshot = snapshot(ownerships);

        for (UserProductOwnership ownership : ownerships) {
            if ("ACTIVE".equals(ownership.getOwnershipStatus()) && !normalizedProductCode.equals(ownership.getProductCode())) {
                ownership.markCorrected();
                userProductOwnershipRepository.save(ownership);
            }
        }

        UserProductOwnership activeOwnership = userProductOwnershipService.claimOwnership(
                userId,
                normalizedProductCode,
                "OWNERSHIP_CORRECTION",
                "MANUAL_OWNERSHIP_CORRECTION",
                null
        );

        List<UserProductOwnership> refreshed = userProductOwnershipRepository.findByUserIdOrderByIdDesc(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        operationAuditLogRepository.save(OperationAuditLog.create(
                SYSTEM_ADMIN_OPERATOR_ID,
                OPERATOR_ROLE,
                MODULE_NAME,
                TARGET_TYPE,
                activeOwnership.getId(),
                "MANUAL_CORRECT",
                beforeSnapshot,
                snapshot(refreshed),
                requestIp,
                note == null ? null : note.trim(),
                now
        ));

        return toResponse(userId, refreshed);
    }

    private OwnershipDetailResponse toResponse(Long userId, List<UserProductOwnership> ownerships) {
        List<OwnershipItemResponse> items = ownerships.stream()
                .sorted(Comparator
                        .comparing((UserProductOwnership item) -> "ACTIVE".equals(item.getOwnershipStatus()) ? 0 : 1)
                        .thenComparing(UserProductOwnership::getEffectiveAt, Comparator.reverseOrder())
                        .thenComparing(UserProductOwnership::getId, Comparator.reverseOrder()))
                .map(item -> new OwnershipItemResponse(
                        item.getId(),
                        item.getProductCode(),
                        item.getOwnershipStatus(),
                        item.getOwnershipSource(),
                        item.getSourceRecordType(),
                        item.getSourceRecordId(),
                        item.getEffectiveAt()
                ))
                .toList();
        return new OwnershipDetailResponse(userId, items);
    }

    private String snapshot(List<UserProductOwnership> ownerships) {
        return ownerships.stream()
                .sorted(Comparator.comparing(UserProductOwnership::getId))
                .map(item -> "id=" + item.getId()
                        + ",product=" + item.getProductCode()
                        + ",status=" + item.getOwnershipStatus()
                        + ",source=" + item.getOwnershipSource()
                        + ",sourceType=" + item.getSourceRecordType()
                        + ",sourceId=" + item.getSourceRecordId())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }
}
