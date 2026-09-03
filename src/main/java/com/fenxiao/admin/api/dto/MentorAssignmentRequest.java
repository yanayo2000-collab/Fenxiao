package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MentorAssignmentRequest(@NotNull Long mentorUserId, @NotBlank String reason) {}
