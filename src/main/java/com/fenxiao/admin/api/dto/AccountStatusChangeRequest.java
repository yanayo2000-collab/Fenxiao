package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountStatusChangeRequest(@NotBlank String action, @NotBlank String reason) {}
