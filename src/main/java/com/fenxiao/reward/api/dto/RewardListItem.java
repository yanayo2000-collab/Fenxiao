package com.fenxiao.reward.api.dto;

import com.fenxiao.reward.domain.RewardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RewardListItem(
        Long beneficiaryUserId,
        Long sourceUserId,
        Integer rewardLevel,
        BigDecimal rewardAmount,
        RewardStatus rewardStatus,
        LocalDateTime calculatedAt
) {
}
