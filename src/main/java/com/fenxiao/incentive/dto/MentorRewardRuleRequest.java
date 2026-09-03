package com.fenxiao.incentive.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
public record MentorRewardRuleRequest(@NotBlank String ruleCode, @NotBlank String milestoneCode,
                                      @NotBlank String platformCode, @NotBlank String countryCode,
                                      String guildId, @PositiveOrZero long amountMinor,
                                      @NotBlank String currencyCode, @PositiveOrZero int freezeDays,
                                      LocalDateTime effectiveFrom) {}
