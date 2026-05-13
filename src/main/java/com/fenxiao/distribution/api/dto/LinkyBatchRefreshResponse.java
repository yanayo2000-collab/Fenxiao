package com.fenxiao.distribution.api.dto;

import java.util.List;

public record LinkyBatchRefreshResponse(long successCount,
                                        long failureCount,
                                        List<FailureItem> failures) {

    public record FailureItem(String linkyAccount, String guildCheckStatus, String remark) {}
}
