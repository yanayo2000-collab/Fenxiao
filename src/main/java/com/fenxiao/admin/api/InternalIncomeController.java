package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.InternalIncomeEventRequest;
import com.fenxiao.admin.api.dto.InternalIncomeEventResponse;
import com.fenxiao.admin.api.dto.LinkyIncomeEventRequest;
import com.fenxiao.admin.service.LinkyIncomeAdapterService;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/internal/distribution")
public class InternalIncomeController {

    private final RewardCalculationService rewardCalculationService;
    private final LinkyIncomeAdapterService linkyIncomeAdapterService;
    private final DistributionAccessGuard distributionAccessGuard;

    public InternalIncomeController(RewardCalculationService rewardCalculationService,
                                    LinkyIncomeAdapterService linkyIncomeAdapterService,
                                    DistributionAccessGuard distributionAccessGuard) {
        this.rewardCalculationService = rewardCalculationService;
        this.linkyIncomeAdapterService = linkyIncomeAdapterService;
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

    @PostMapping("/linky/income-events")
    public InternalIncomeEventResponse acceptLinkyIncomeEvent(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                                              @RequestHeader(value = "X-Linky-Timestamp", required = false) String linkyTimestamp,
                                                              @RequestHeader(value = "X-Linky-Signature", required = false) String linkySignature,
                                                              @Valid @RequestBody LinkyIncomeEventRequest request) {
        distributionAccessGuard.assertInternalToken(token);
        distributionAccessGuard.assertLinkySignature(
                linkyTimestamp,
                linkySignature,
                request.linkyOrderId(),
                request.userId(),
                request.incomeAmount().toPlainString(),
                request.currencyCode(),
                request.paidAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        return linkyIncomeAdapterService.accept(request);
    }
}
