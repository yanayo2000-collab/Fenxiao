package com.fenxiao.incentive.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record LeadershipPolicyRequest(@NotBlank String policyCode, @NotBlank String platformCode,
                                      @NotBlank String countryCode, String guildId,
                                      @Positive int requiredValidStarts, @PositiveOrZero int requiredWithdrawEligible,
                                      @PositiveOrZero int requiredActive7d,
                                      @DecimalMin("0.0") @DecimalMax("0.30") BigDecimal profitShareRate,
                                      LocalDateTime effectiveFrom) {}
