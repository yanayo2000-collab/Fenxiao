package com.fenxiao.reward.api.dto;

import java.util.List;

public record RewardListResponse(
        List<RewardListItem> items,
        long total
) {
}
