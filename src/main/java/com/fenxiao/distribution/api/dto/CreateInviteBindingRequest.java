package com.fenxiao.distribution.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateInviteBindingRequest(
        @NotBlank String inviteCode,
        @NotBlank
        @Pattern(regexp = "^\\+?[1-9][0-9]{6,19}$", message = "whatsapp number invalid")
        String whatsappNumber,
        @NotBlank
        @Pattern(regexp = "^[0-9]{8}$", message = "linky account must be 8 digits")
        String linkyAccount
) {
}
