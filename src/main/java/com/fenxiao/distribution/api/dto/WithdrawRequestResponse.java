package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;

public record WithdrawRequestResponse(
        String requestNo,
        Long userId,
        BigDecimal requestedDiamondAmount,
        String requestStatus,
        String requestWeek,
        String requestedAt
) {
}
