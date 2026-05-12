package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneLoginRequest(@NotBlank String phoneNumber,
                                @NotBlank String verificationCode,
                                String inviteCode,
                                String countryCode,
                                String languageCode) {}
