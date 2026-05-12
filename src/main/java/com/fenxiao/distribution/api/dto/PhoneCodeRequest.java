package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PhoneCodeRequest(@NotBlank String phoneNumber) {}
