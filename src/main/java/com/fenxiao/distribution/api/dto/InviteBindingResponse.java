package com.fenxiao.distribution.api.dto;

public record InviteBindingResponse(
        Long id,
        Long inviterUserId,
        String inviteCode,
        String whatsappNumber,
        String linkyAccount,
        String bindStatus,
        String submittedAt
) {
}
