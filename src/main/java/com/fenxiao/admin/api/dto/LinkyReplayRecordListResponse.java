package com.fenxiao.admin.api.dto;

import java.util.List;

public record LinkyReplayRecordListResponse(
        List<LinkyReplayRecordItem> items,
        long total,
        int page,
        int size
) {
}
