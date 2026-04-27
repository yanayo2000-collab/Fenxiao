package com.fenxiao.distribution.api.dto;

public record InviteBindingResponse(
        Long id,
        String productCode,
        Long inviterUserId,
        String inviteCode,
        String whatsappNumber,
        String linkyAccount,
        String bindStatus,
        String submittedAt
) {
}
