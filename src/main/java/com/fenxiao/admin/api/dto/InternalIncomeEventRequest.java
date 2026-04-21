package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InternalIncomeEventRequest(
        @NotBlank String sourceEventId,
        @NotNull @Positive Long userId,
        @NotNull @Positive BigDecimal incomeAmount,
        @NotBlank String currencyCode,
        @NotNull LocalDateTime eventTime
) {
}
