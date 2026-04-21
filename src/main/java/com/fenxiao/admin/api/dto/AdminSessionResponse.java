package com.fenxiao.admin.api.dto;

public record AdminSessionResponse(
        String sessionToken,
        String expiresAt
) {
}
