package com.fenxiao.admin.api.dto;

import java.time.LocalDateTime;

public record AdminAccountResponse(Long id,String username,String displayName,String role,boolean enabled,
                                   String platformScope,String guildScope,String regionScope,boolean mustChangePassword,
                                   LocalDateTime lastLoginAt,LocalDateTime passwordChangedAt,
                                   LocalDateTime passwordExpiresAt,LocalDateTime lockedUntil,long activeSessions){}
