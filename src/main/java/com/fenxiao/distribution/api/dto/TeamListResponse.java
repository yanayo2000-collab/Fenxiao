package com.fenxiao.distribution.api.dto;

import java.util.List;

public record TeamListResponse(
        List<TeamMemberItem> items,
        long total
) {
}
