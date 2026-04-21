package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;

public record DistributionHomeResponse(
        Long userId,
        String inviteCode,
        Long inviterUserId,
        long invitedUsers,
        long effectiveUsers,
        BigDecimal totalReward,
        BigDecimal frozenReward,
        BigDecimal availableReward,
        BigDecimal riskHoldReward
) {
}
