package com.fenxiao.admin.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LinkyWebhookLogItem(
        Long id,
        String linkyOrderId,
        String sourceEventId,
        Long userId,
        BigDecimal incomeAmount,
        String currencyCode,
        LocalDateTime paidAt,
        LocalDateTime requestReceivedAt,
        String internalTokenStatus,
        String signatureStatus,
        String replayStatus,
        String requestStatus,
        String failureReason
) {
}
