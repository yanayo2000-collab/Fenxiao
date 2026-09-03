package com.fenxiao.admin.api.dto;
import java.time.LocalDateTime;
public record AdminSecurityEventResponse(Long id,Long accountId,String username,String eventType,boolean success,String ipAddress,String userAgent,String detail,LocalDateTime occurredAt){}
