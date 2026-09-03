package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(@NotBlank String teamCode, @NotBlank String teamName, @NotBlank String countryCode, Long leaderUserId) {}
