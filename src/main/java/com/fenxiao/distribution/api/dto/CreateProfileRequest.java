package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProfileRequest(
        @NotNull @Positive Long userId,
        @NotBlank String countryCode,
        @NotBlank String languageCode,
        String inviteCode
) {
}
