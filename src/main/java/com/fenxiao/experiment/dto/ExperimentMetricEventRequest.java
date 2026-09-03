package com.fenxiao.experiment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExperimentMetricEventRequest(
        @NotNull Long userId,
        @NotBlank String metricCode,
        @NotNull BigDecimal metricValue,
        @NotBlank String sourceSystem,
        @NotBlank String sourceEventId,
        @NotNull LocalDateTime occurredAt) {}
