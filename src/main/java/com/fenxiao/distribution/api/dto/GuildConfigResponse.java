package com.fenxiao.distribution.api.dto;

public record GuildConfigResponse(Long id,
                                  String productCode,
                                  Long inviterUserId,
                                  String guildId,
                                  String guildName,
                                  String guildInviteCode,
                                  boolean enabled) {}
