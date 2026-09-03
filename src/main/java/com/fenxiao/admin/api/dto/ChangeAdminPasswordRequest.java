package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeAdminPasswordRequest(@NotBlank String currentPassword,@NotBlank String newPassword){}
