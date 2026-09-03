package com.fenxiao.experiment.dto;

import jakarta.validation.constraints.NotBlank;

public record ExperimentStatusRequest(@NotBlank String status, String reason) {}
