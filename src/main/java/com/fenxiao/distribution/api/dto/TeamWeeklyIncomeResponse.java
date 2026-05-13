package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeamWeeklyIncomeResponse(
        Long userId,
        String currentWeek,
        String previousWeek,
        BigDecimal currentWeekTeamIncome,
        BigDecimal previousWeekTeamIncome,
        List<TeamWeeklyIncomeItem> items
) {
}
