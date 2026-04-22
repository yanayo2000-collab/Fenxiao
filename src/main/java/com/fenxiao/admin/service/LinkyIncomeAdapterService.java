package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.InternalIncomeEventResponse;
import com.fenxiao.admin.api.dto.LinkyIncomeEventRequest;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import org.springframework.stereotype.Service;

@Service
public class LinkyIncomeAdapterService {

    private final RewardCalculationService rewardCalculationService;

    public LinkyIncomeAdapterService(RewardCalculationService rewardCalculationService) {
        this.rewardCalculationService = rewardCalculationService;
    }

    public InternalIncomeEventResponse accept(LinkyIncomeEventRequest request) {
        String sourceEventId = "LINKY:" + request.linkyOrderId().trim();
        IncomeProcessStatus status = rewardCalculationService.processIncomeEvent(
                sourceEventId,
                request.userId(),
                request.incomeAmount(),
                request.currencyCode(),
                request.paidAt()
        );
        return new InternalIncomeEventResponse(sourceEventId, status);
    }
}
