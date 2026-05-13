package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;

public record TeamWeeklyIncomeItem(
        Long userId,
        String inviteCode,
        boolean effectiveUser,
        BigDecimal currentWeekIncome,
        BigDecimal previousWeekIncome
) {
}
