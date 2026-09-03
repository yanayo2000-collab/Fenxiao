package com.fenxiao.platform.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PlatformMilestonePolicyRequest(@NotBlank String platformCode, @NotBlank String guildId,
                                             @NotBlank String countryCode, @NotNull BigDecimal minimumWithdrawableAmount,
                                             @NotBlank String currencyCode, LocalDateTime effectiveFrom) {}
