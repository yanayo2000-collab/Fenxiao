package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;

public record WeeklyIncomeStatsResponse(
        Long userId,
        String currentWeek,
        String previousWeek,
        BigDecimal currentWeekIncome,
        BigDecimal previousWeekIncome,
        BigDecimal currentWeekReward,
        BigDecimal previousWeekReward
) {
}
