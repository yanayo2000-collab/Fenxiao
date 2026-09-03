package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchRiskEventActionRequest(
        @NotEmpty @Size(max = 100) List<@NotNull Long> riskEventIds,
        @NotNull RiskEventAction action,
        @Size(max = 255) String note
) {
}
