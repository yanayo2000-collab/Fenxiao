package com.fenxiao.admin.api.dto;

import com.fenxiao.risk.domain.RiskStatus;

import java.time.LocalDateTime;

public record RiskEventListItem(
        Long id,
        Long userId,
        String riskType,
        Integer riskLevel,
        RiskStatus riskStatus,
        String detailJson,
        LocalDateTime detectedAt
) {
}
