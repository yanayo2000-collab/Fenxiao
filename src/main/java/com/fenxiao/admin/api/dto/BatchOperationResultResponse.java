package com.fenxiao.admin.api.dto;

import java.util.List;

public record BatchOperationResultResponse(
        int successCount,
        int failureCount,
        List<Item> items
) {
    public record Item(
            String targetId,
            boolean success,
            String status,
            String message
    ) {
    }
}
