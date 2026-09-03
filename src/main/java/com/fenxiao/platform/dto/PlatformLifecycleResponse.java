package com.fenxiao.platform.dto;
import com.fenxiao.platform.entity.PlatformLifecycleSnapshot;
import java.math.BigDecimal;
public record PlatformLifecycleResponse(boolean bindingVerified, boolean valid72HourStart, String firstIncomeAt, String firstWithdrawEligibleAt,
                                        int consecutiveActiveDays, boolean active7Days, boolean active30Days,
                                        BigDecimal cumulativeNetIncome, boolean shadowOnly) {
    public static PlatformLifecycleResponse from(PlatformLifecycleSnapshot value) {
        return new PlatformLifecycleResponse(value.isBindingVerified(), value.isValid72HourStart(),
                value.getFirstIncomeAt() == null ? null : value.getFirstIncomeAt().toString(),
                value.getFirstWithdrawEligibleAt() == null ? null : value.getFirstWithdrawEligibleAt().toString(),
                value.getConsecutiveActiveDays(), value.isConsecutive7DayActive(), value.isConsecutive30DayActive(),
                value.getCumulativeNetIncome(), value.isShadowOnly());
    }
}
