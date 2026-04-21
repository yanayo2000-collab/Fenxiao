package com.fenxiao.distribution.api.dto;

public record ProfileResponse(
        Long userId,
        String inviteCode,
        String countryCode,
        String languageCode
) {
}
