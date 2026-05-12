package com.fenxiao.reward.api.dto;

import java.util.List;

public record RewardSummaryResponse(
        Long userId,
        List<RewardTierSummaryItem> tiers
) {
}
