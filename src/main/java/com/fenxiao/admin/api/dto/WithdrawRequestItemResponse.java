package com.fenxiao.admin.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record WithdrawRequestItemResponse(
        String requestNo,
        Long userId,
        BigDecimal requestedDiamondAmount,
        String requestStatus,
        String requestWeek,
        String requestedAt
) {
    public static WithdrawRequestListResponse list(List<WithdrawRequestItemResponse> items, long total, int page, int size) {
        return new WithdrawRequestListResponse(items, total, page, size);
    }
}
