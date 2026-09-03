package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record WithdrawalReconciliationRequest(@NotBlank String status,
                                              String externalReference,
                                              BigDecimal externalAmount,
                                              String currencyCode,
                                              String details) {}
