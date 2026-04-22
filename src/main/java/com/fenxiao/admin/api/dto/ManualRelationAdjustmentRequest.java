package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.Size;

public record ManualRelationAdjustmentRequest(
        Long level1InviterId,
        @Size(max = 255) String note
) {
}
