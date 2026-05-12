package com.fenxiao.admin.api.dto;

public record LinkyEligibilityCheckRequest(
        String linkyAccount,
        String guildId,
        String guildName,
        String result,
        String remark
) {
}
