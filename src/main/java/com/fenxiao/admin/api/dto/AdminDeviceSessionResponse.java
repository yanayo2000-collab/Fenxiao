package com.fenxiao.admin.api.dto;

import java.time.LocalDateTime;

public record AdminDeviceSessionResponse(Long id,boolean current,boolean rememberMe,LocalDateTime issuedAt,
                                         LocalDateTime lastSeenAt,LocalDateTime expiresAt,String ipAddress,String userAgent){}
