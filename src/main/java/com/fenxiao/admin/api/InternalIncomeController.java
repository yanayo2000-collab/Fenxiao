package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.InternalIncomeEventRequest;
import com.fenxiao.admin.api.dto.InternalIncomeEventResponse;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/distribution")
public class InternalIncomeController {

    private final RewardCalculationService rewardCalculationService;
    private final DistributionAccessGuard distributionAccessGuard;

    public InternalIncomeController(RewardCalculationService rewardCalculationService,
                                    DistributionAccessGuard distributionAccessGuard) {
        this.rewardCalculationService = rewardCalculationService;
        this.distributionAccessGuard = distributionAccessGuard;
    }

    @PostMapping("/income-events")
    public InternalIncomeEventResponse acceptIncomeEvent(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                                         @Valid @RequestBody InternalIncomeEventRequest request) {
        distributionAccessGuard.assertInternalToken(token);
        IncomeProcessStatus status = rewardCalculationService.processIncomeEvent(
                request.sourceEventId(),
                request.userId(),
                request.incomeAmount(),
                request.currencyCode(),
                request.eventTime()
        );
        return new InternalIncomeEventResponse(request.sourceEventId(), status);
    }
}
