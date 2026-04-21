package com.fenxiao.admin.api.dto;

import com.fenxiao.distribution.domain.BindSource;
import com.fenxiao.distribution.domain.LockStatus;

import java.time.LocalDateTime;

public record RelationDetailResponse(
        Long userId,
        Long level1InviterId,
        Long level2InviterId,
        Long level3InviterId,
        BindSource bindSource,
        LockStatus lockStatus,
        LocalDateTime bindTime,
        LocalDateTime lockTime,
        String countryCode,
        boolean crossCountry
) {
}
