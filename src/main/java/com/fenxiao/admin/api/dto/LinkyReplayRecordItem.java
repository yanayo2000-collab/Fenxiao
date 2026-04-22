package com.fenxiao.admin.api.dto;

import java.time.LocalDateTime;

public record LinkyReplayRecordItem(
        Long id,
        String requestFingerprint,
        String linkyOrderId,
        String sourceEventId,
        Long userId,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt,
        Integer hitCount,
        String latestRequestStatus,
        String latestFailureReason
) {
}
