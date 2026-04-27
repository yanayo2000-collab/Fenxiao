package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.LinkyReplayRecordItem;
import com.fenxiao.admin.api.dto.LinkyReplayRecordListResponse;
import com.fenxiao.linky.entity.LinkyReplayRecord;
import com.fenxiao.linky.repository.LinkyReplayRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class LinkyReplayRecordService {

    private final LinkyReplayRecordRepository linkyReplayRecordRepository;
    private final Clock clock;
    private final AdminProductScopeService adminProductScopeService;

    public LinkyReplayRecordService(LinkyReplayRecordRepository linkyReplayRecordRepository,
                                    Clock clock,
                                    AdminProductScopeService adminProductScopeService) {
        this.linkyReplayRecordRepository = linkyReplayRecordRepository;
        this.clock = clock;
        this.adminProductScopeService = adminProductScopeService;
    }

    @Transactional
    public ReplayRecordResult register(String linkyOrderId,
                                       Long userId,
                                       String incomeAmount,
                                       String currencyCode,
                                       String paidAt,
                                       String linkyTimestamp,
                                       String linkySignature,
                                       String latestRequestStatus,
                                       String latestFailureReason) {
        String fingerprint = buildFingerprint(linkyOrderId, userId, incomeAmount, currencyCode, paidAt, linkyTimestamp, linkySignature);
        String sourceEventId = "LINKY:" + linkyOrderId.trim();
        LocalDateTime now = LocalDateTime.now(clock);
        return linkyReplayRecordRepository.findByRequestFingerprint(fingerprint)
                .map(record -> {
                    record.markSeenAgain(now, latestRequestStatus, latestFailureReason);
                    linkyReplayRecordRepository.save(record);
                    return new ReplayRecordResult(fingerprint, "REPLAYED", record.getHitCount());
                })
                .orElseGet(() -> {
                    LinkyReplayRecord created = LinkyReplayRecord.create(
                            fingerprint,
                            linkyOrderId.trim(),
                            sourceEventId,
                            userId,
                            now,
                            latestRequestStatus,
                            latestFailureReason
                    );
                    linkyReplayRecordRepository.save(created);
                    return new ReplayRecordResult(fingerprint, "FIRST_SEEN", created.getHitCount());
                });
    }

    public LinkyReplayRecordListResponse getRecords(String linkyOrderId,
                                                    Long userId,
                                                    int page,
                                                    int size,
                                                    String productCode) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        String normalizedProductCode = adminProductScopeService.normalizeProductCode(productCode);
        Page<LinkyReplayRecord> records;
        if (normalizedProductCode == null) {
            records = linkyReplayRecordRepository.findForAdmin(normalizeFilter(linkyOrderId), userId, PageRequest.of(page, size));
        } else {
            List<Long> scopedUserIds = adminProductScopeService.resolveScopedUserIds(normalizedProductCode);
            if (scopedUserIds.isEmpty()) {
                return new LinkyReplayRecordListResponse(List.of(), 0, page, size);
            }
            if (userId != null) {
                if (!scopedUserIds.contains(userId)) {
                    return new LinkyReplayRecordListResponse(List.of(), 0, page, size);
                }
                scopedUserIds = List.of(userId);
            }
            records = linkyReplayRecordRepository.findForAdminByUserIdIn(
                    scopedUserIds,
                    normalizeFilter(linkyOrderId),
                    userId,
                    PageRequest.of(page, size)
            );
        }
        List<LinkyReplayRecordItem> items = new ArrayList<>();
        for (LinkyReplayRecord record : records.getContent()) {
            items.add(new LinkyReplayRecordItem(
                    record.getId(),
                    record.getRequestFingerprint(),
                    record.getLinkyOrderId(),
                    record.getSourceEventId(),
                    record.getUserId(),
                    record.getFirstSeenAt(),
                    record.getLastSeenAt(),
                    record.getHitCount(),
                    record.getLatestRequestStatus(),
                    record.getLatestFailureReason()
            ));
        }
        return new LinkyReplayRecordListResponse(items, records.getTotalElements(), page, size);
    }

    private String buildFingerprint(String linkyOrderId,
                                    Long userId,
                                    String incomeAmount,
                                    String currencyCode,
                                    String paidAt,
                                    String linkyTimestamp,
                                    String linkySignature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = normalize(linkyTimestamp) + "."
                    + normalize(linkySignature) + "."
                    + linkyOrderId.trim() + "."
                    + userId + "."
                    + incomeAmount + "."
                    + currencyCode.trim().toUpperCase() + "."
                    + paidAt;
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to build linky replay fingerprint", exception);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record ReplayRecordResult(String fingerprint, String status, Integer hitCount) {
    }
}
