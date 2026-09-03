package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawalPaymentRequest(@NotBlank String paymentChannel,
                                       String paymentReference,
                                       String evidenceUri,
                                       String evidenceHash,
                                       String failureReason) {}
