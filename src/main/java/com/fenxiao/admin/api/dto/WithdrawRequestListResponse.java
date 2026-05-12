package com.fenxiao.admin.api.dto;

import java.util.List;

public record WithdrawRequestListResponse(
        List<WithdrawRequestItemResponse> items,
        long total,
        int page,
        int size
) {
}
