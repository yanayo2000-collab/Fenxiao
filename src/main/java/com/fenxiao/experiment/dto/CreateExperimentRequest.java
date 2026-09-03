package com.fenxiao.experiment.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateExperimentRequest(
        @NotBlank String experimentCode,
        @NotBlank String experimentName,
        @Min(1) @Max(100) int plannedSampleSize,
        @NotBlank String primaryMetricCode,
        @NotNull LocalDateTime enrollmentStartsAt,
        @NotNull LocalDateTime enrollmentEndsAt,
        @NotNull LocalDateTime observationEndsAt) {}
