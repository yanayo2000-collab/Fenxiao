package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        Boolean rememberMe
) {
    public boolean remembersDevice() { return Boolean.TRUE.equals(rememberMe); }
}
