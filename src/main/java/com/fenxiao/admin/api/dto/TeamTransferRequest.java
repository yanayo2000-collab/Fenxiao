package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record TeamTransferRequest(@NotNull Long teamId, LocalDateTime effectiveAt, @NotBlank String reason) {}
