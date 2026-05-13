package com.fenxiao.admin.api.dto;

public record WithdrawRequestActionRequest(
        Long operatorId,
        String operatorRole,
        String remark
) {
}
