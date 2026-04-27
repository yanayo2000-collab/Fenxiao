package com.fenxiao.admin.api.dto;

import java.util.List;

public record OwnershipDetailResponse(
        Long userId,
        List<OwnershipItemResponse> items
) {
}
