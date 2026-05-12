package com.fenxiao.reward.api.dto;

import java.math.BigDecimal;

public record RewardTierSummaryItem(
        Integer rewardLevel,
        String businessLevelLabel,
        long rewardCount,
        BigDecimal rewardAmount
) {
}
