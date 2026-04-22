package com.fenxiao.admin.api.dto;

import java.util.List;

public record LinkyWebhookLogListResponse(
        List<LinkyWebhookLogItem> items,
        long total,
        int page,
        int size
) {
}
