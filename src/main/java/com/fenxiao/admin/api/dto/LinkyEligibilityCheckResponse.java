package com.fenxiao.admin.api.dto;

public record LinkyEligibilityCheckResponse(
        String linkyAccount,
        String guildId,
        String guildName,
        String guildCheckStatus,
        String registrationEligibility,
        String checkedAt,
        String remark
) {
}
