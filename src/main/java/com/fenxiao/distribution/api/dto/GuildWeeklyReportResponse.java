package com.fenxiao.distribution.api.dto;

import java.math.BigDecimal;

public record GuildWeeklyReportResponse(String productCode,
                                        String guildId,
                                        String week,
                                        long registeredUsers,
                                        BigDecimal incomeAmount,
                                        BigDecimal rewardAmount) {}
