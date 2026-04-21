package com.fenxiao.distribution.api.dto;

import com.fenxiao.distribution.domain.LockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TeamMemberItem(
        Long userId,
        String inviteCode,
        String countryCode,
        boolean effectiveUser,
        BigDecimal confirmedIncomeTotal,
        LockStatus lockStatus,
        LocalDateTime bindTime
) {
}
