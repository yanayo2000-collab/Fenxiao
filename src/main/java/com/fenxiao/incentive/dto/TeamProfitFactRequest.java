package com.fenxiao.incentive.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record TeamProfitFactRequest(@NotBlank String sourceEventId, @NotNull Long teamId,
                                    @NotBlank String platformCode, @NotNull LocalDate periodStart,
                                    @NotNull LocalDate periodEnd, long businessIncomeMinor,
                                    long directCostMinor, long recruiterRewardMinor, long mentorRewardMinor,
                                    long paymentAdjustmentMinor, @NotBlank String currencyCode,
                                    @NotBlank String sourceSystem) {}
