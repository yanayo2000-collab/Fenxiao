package com.fenxiao.admin.api.dto;

import java.util.List;

public record LinkyRegisteredAccountPageResponse(
        List<Item> items,
        int page,
        int totalPages,
        long totalElements
) {
    public record Item(String linkyAccount, Long userId) {
    }
}
