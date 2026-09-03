package com.fenxiao.admin.api.dto;

public record RewardEngineReportResponse(
        boolean engineEnabled,
        String engineVersion,
        long processedIncomeEvents,
        long generatedDirectRewards,
        long legacyRewardCandidates,
        long blockedLegacyRewardCandidates,
        long newMultilevelRewards,
        long historicalLegacyRewards,
        boolean safetyGatePassed
) {
}
