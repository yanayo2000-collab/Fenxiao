package com.fenxiao.admin.api.dto;

import com.fenxiao.reward.domain.IncomeProcessStatus;

public record InternalIncomeEventResponse(
        String sourceEventId,
        IncomeProcessStatus status
) {
}
