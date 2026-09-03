package com.fenxiao.platform.dto;
import com.fenxiao.platform.entity.PlatformAccountBinding;
public record PlatformBindingResponse(Long id, Long userId, String platformCode, String platformUserId,
                                      String status, String submittedAt, String officialGuildId,
                                      String officialJoinedAt, String rejectionCode, String rejectionReason,
                                      int version) {
    public static PlatformBindingResponse from(PlatformAccountBinding value) {
        return new PlatformBindingResponse(value.getId(), value.getUserId(), value.getPlatformCode(), value.getPlatformUserId(),
                value.getBindingStatus().name(), value.getSubmittedAt().toString(), value.getOfficialGuildId(),
                value.getOfficialJoinedAt() == null ? null : value.getOfficialJoinedAt().toString(),
                value.getRejectionCode(), value.getRejectionReason(), value.getVersionNo());
    }
}

