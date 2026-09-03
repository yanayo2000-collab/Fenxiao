package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminAccountRequest(@NotBlank String username,@NotBlank String displayName,@NotBlank String role,
                                        String platformScope,String guildScope,String regionScope){}
