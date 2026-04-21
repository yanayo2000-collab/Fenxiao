package com.fenxiao.admin.api.dto;

import java.math.BigDecimal;

public record OverviewReportResponse(
        long invitedUsers,
        long effectiveUsers,
        BigDecimal rewardTotal,
        BigDecimal frozenRewardTotal,
        BigDecimal availableRewardTotal,
        long riskEventCount
) {
}
