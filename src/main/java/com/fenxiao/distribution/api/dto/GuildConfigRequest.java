package com.fenxiao.distribution.api.dto;

public record GuildConfigRequest(String productCode,
                                 Long inviterUserId,
                                 String guildId,
                                 String guildName,
                                 String guildInviteCode,
                                 Boolean enabled) {}
