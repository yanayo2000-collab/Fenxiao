package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RiskEventActionRequest(
        @NotNull RiskEventAction action,
        @Size(max = 255) String note
) {
}
