package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchWithdrawRequestActionRequest(
        @NotEmpty @Size(max = 100) List<@NotBlank String> requestNos,
        @NotNull WithdrawBatchAction action,
        @Size(max = 255) String remark
) {
}
