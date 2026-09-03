package com.fenxiao.platform.dto;
import jakarta.validation.constraints.NotBlank;
public record SubmitPlatformBindingRequest(@NotBlank String platformCode, @NotBlank String platformUserId) {}

