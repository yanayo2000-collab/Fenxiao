package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualOwnershipCorrectionRequest(
        @NotBlank String productCode,
        @Size(max = 255) String note
) {
}
