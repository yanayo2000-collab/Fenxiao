package com.fenxiao.admin.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LinkyIncomeEventRequest(
        @NotBlank @JsonAlias({"orderId", "externalOrderId"}) String linkyOrderId,
        @NotNull @Positive @JsonAlias({"memberId", "beneficiaryUserId"}) Long userId,
        @NotNull @Positive @JsonAlias({"commissionAmount", "amount"}) BigDecimal incomeAmount,
        @NotBlank @JsonAlias({"currency", "currency"}) String currencyCode,
        @NotNull @JsonAlias({"settledAt", "paidTime"}) LocalDateTime paidAt
) {
}
