package com.fenxiao.admin.api.dto;

import java.util.List;

public record RiskEventListResponse(
        List<RiskEventListItem> items,
        long total,
        int page,
        int size
) {
}
