package com.fenxiao.admin.api.dto;

public record AdminSessionResponse(
        String sessionToken,
        String expiresAt,
        String username,
        String displayName,
        String role
) {
}
