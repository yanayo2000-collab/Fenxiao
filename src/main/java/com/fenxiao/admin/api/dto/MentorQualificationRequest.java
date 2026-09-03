package com.fenxiao.admin.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MentorQualificationRequest(@NotBlank String countryCode, @NotBlank String languageCode, @Min(1) int maxActiveStudents) {}
