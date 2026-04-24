package com.fenxiao.distribution.api.dto;

public record IssueInviteCodeResponse(
        Long userId,
        String productCode,
        String whatsappNumber,
        String appAccount,
        String inviteCode,
        String countryCode,
        String languageCode,
        String accessToken,
        String issuedAt
) {
}