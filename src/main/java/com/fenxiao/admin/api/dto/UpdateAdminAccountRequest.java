package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAdminAccountRequest(@NotBlank String displayName,@NotBlank String role,boolean enabled,
                                        String platformScope,String guildScope,String regionScope){}
