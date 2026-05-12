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
        BigDecimal riskHoldReward,
        long directInvitedUsers,
        long secondLevelInvitedUsers,
        long thirdLevelInvitedUsers,
        long totalTeamUsers,
        long directEffectiveUsers,
        long secondLevelEffectiveUsers,
        long thirdLevelEffectiveUsers,
        long totalEffectiveUsers
) {
}
