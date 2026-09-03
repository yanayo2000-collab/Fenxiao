package com.fenxiao.experiment.dto;

import jakarta.validation.constraints.*;

public record EnrollParticipantRequest(
        @NotNull Long userId,
        @NotBlank String cohortCode,
        @NotBlank String eligibilitySnapshot) {}
