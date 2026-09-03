package com.fenxiao.platform.dto;
import com.fenxiao.platform.domain.PlatformFactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PlatformBusinessFactRequest(
        @NotBlank String sourceEventId,
        @NotBlank String platformCode,
        @NotBlank String platformUserId,
        @NotNull PlatformFactType factType,
        BigDecimal amount,
        String currencyCode,
        @NotNull LocalDateTime occurredAt,
        String guildId,
        @NotBlank String sourceSystem,
        String sourceVersion,
        String payloadHash
) {}

