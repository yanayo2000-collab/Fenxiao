package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.InternalIncomeEventRequest;
import com.fenxiao.admin.api.dto.InternalIncomeEventResponse;
import com.fenxiao.admin.api.dto.LinkyIncomeEventRequest;
import com.fenxiao.admin.api.dto.LinkyRegisteredAccountPageResponse;
import com.fenxiao.admin.service.LinkyIncomeAdapterService;
import com.fenxiao.admin.service.LinkyReplayRecordService;
import com.fenxiao.admin.service.LinkyWebhookLogService;
import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.distribution.repository.LinkyAccountBindingRepository;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.service.RewardCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/internal/distribution")
public class InternalIncomeController {

    private final RewardCalculationService rewardCalculationService;
    private final LinkyIncomeAdapterService linkyIncomeAdapterService;
    private final LinkyReplayRecordService linkyReplayRecordService;
    private final LinkyWebhookLogService linkyWebhookLogService;
    private final DistributionAccessGuard distributionAccessGuard;
    private final LinkyAccountBindingRepository linkyAccountBindingRepository;

    public InternalIncomeController(RewardCalculationService rewardCalculationService,
                                    LinkyIncomeAdapterService linkyIncomeAdapterService,
                                    LinkyReplayRecordService linkyReplayRecordService,
                                    LinkyWebhookLogService linkyWebhookLogService,
                                    DistributionAccessGuard distributionAccessGuard,
                                    LinkyAccountBindingRepository linkyAccountBindingRepository) {
        this.rewardCalculationService = rewardCalculationService;
        this.linkyIncomeAdapterService = linkyIncomeAdapterService;
        this.linkyReplayRecordService = linkyReplayRecordService;
        this.linkyWebhookLogService = linkyWebhookLogService;
        this.distributionAccessGuard = distributionAccessGuard;
        this.linkyAccountBindingRepository = linkyAccountBindingRepository;
    }

    @GetMapping("/linky/registered-accounts")
    public LinkyRegisteredAccountPageResponse registeredLinkyAccounts(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {
        distributionAccessGuard.assertInternalToken(token);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 1000));
        var result = linkyAccountBindingRepository.findByUserIdIsNotNullOrderByIdAsc(
                PageRequest.of(safePage, safeSize));
        return new LinkyRegisteredAccountPageResponse(
                result.getContent().stream()
                        .map(item -> new LinkyRegisteredAccountPageResponse.Item(
                                item.getLinkyAccount(), item.getUserId()))
                        .toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
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
                                                              @Valid @RequestBody LinkyIncomeEventRequest request,
                                                              HttpServletRequest httpServletRequest) {
        DistributionAccessGuard.InternalTokenCheckResult tokenCheck = distributionAccessGuard.inspectInternalToken(token);
        DistributionAccessGuard.LinkyRequestCheckResult linkyCheck = distributionAccessGuard.inspectLinkySignature(
                linkyTimestamp,
                linkySignature,
                request.linkyOrderId(),
                request.userId(),
                request.incomeAmount().toPlainString(),
                request.currencyCode(),
                request.paidAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
        InternalIncomeEventResponse response = null;
        RuntimeException failure = null;
        LinkyReplayRecordService.ReplayRecordResult replayRecordResult = null;
        try {
            if (!tokenCheck.allowed()) {
                throw new ForbiddenException(tokenCheck.message());
            }
            if (!linkyCheck.allowed()) {
                throw new ForbiddenException(linkyCheck.message());
            }
            response = linkyIncomeAdapterService.accept(request);
            return response;
        } catch (RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            if (tokenCheck.allowed() && linkyCheck.allowed()) {
                String finalRequestStatus = response != null ? response.status().name() : (failure instanceof ForbiddenException ? "REJECTED" : "FAILED");
                replayRecordResult = linkyReplayRecordService.register(
                        request.linkyOrderId(),
                        request.userId(),
                        request.incomeAmount().toPlainString(),
                        request.currencyCode(),
                        request.paidAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        linkyTimestamp,
                        linkySignature,
                        finalRequestStatus,
                        failure == null ? null : failure.getMessage()
                );
            }
            linkyWebhookLogService.record(
                    request,
                    linkyTimestamp,
                    httpServletRequest.getRemoteAddr(),
                    tokenCheck,
                    linkyCheck,
                    replayRecordResult,
                    response,
                    failure
            );
        }
    }
}
