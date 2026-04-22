package com.fenxiao.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenxiao.admin.api.dto.LinkyWebhookLogItem;
import com.fenxiao.admin.api.dto.LinkyWebhookLogListResponse;
import com.fenxiao.admin.api.dto.LinkyIncomeEventRequest;
import com.fenxiao.admin.api.dto.InternalIncomeEventResponse;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.linky.entity.LinkyWebhookLog;
import com.fenxiao.linky.repository.LinkyWebhookLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LinkyWebhookLogService {

    private final LinkyWebhookLogRepository linkyWebhookLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LinkyWebhookLogService(LinkyWebhookLogRepository linkyWebhookLogRepository,
                                  ObjectMapper objectMapper,
                                  Clock clock) {
        this.linkyWebhookLogRepository = linkyWebhookLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void record(LinkyIncomeEventRequest request,
                       String linkyTimestamp,
                       String linkySignature,
                       String requestIp,
                       DistributionAccessGuard.InternalTokenCheckResult tokenCheck,
                       DistributionAccessGuard.LinkyRequestCheckResult linkyCheck,
                       LinkyReplayRecordService.ReplayRecordResult replayRecordResult,
                       InternalIncomeEventResponse response,
                       RuntimeException failure) {
        String requestStatus = response != null ? response.status().name() : classifyFailure(failure);
        String failureReason = failure == null ? null : trimMessage(failure.getMessage());
        String sourceEventId = response != null ? response.sourceEventId() : buildSourceEventId(request.linkyOrderId());
        linkyWebhookLogRepository.save(LinkyWebhookLog.create(
                request.linkyOrderId().trim(),
                sourceEventId,
                request.userId(),
                request.incomeAmount(),
                request.currencyCode().trim().toUpperCase(),
                request.paidAt(),
                LocalDateTime.now(clock),
                linkyTimestamp,
                linkySignature,
                tokenCheck.status(),
                linkyCheck.signatureStatus(),
                linkyCheck.replayStatus(),
                replayRecordResult != null ? replayRecordResult.status() : "NOT_RECORDED",
                replayRecordResult != null ? replayRecordResult.fingerprint() : null,
                replayRecordResult != null ? replayRecordResult.hitCount() : null,
                requestStatus,
                failureReason,
                requestIp,
                serializePayload(request)
        ));
    }

    public LinkyWebhookLogListResponse getLogs(String linkyOrderId, Long userId, String requestStatus, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        Page<LinkyWebhookLog> logs = linkyWebhookLogRepository.findForAdmin(
                normalize(linkyOrderId),
                userId,
                normalize(requestStatus),
                PageRequest.of(page, size)
        );
        List<LinkyWebhookLogItem> items = new ArrayList<>();
        for (LinkyWebhookLog log : logs.getContent()) {
            items.add(new LinkyWebhookLogItem(
                    log.getId(),
                    log.getLinkyOrderId(),
                    log.getSourceEventId(),
                    log.getUserId(),
                    log.getIncomeAmount(),
                    log.getCurrencyCode(),
                    log.getPaidAt(),
                    log.getRequestReceivedAt(),
                    log.getInternalTokenStatus(),
                    log.getSignatureStatus(),
                    log.getReplayStatus(),
                    log.getReplayRecordStatus(),
                    log.getReplayHitCount(),
                    log.getRequestStatus(),
                    log.getFailureReason()
            ));
        }
        return new LinkyWebhookLogListResponse(items, logs.getTotalElements(), page, size);
    }

    private String serializePayload(LinkyIncomeEventRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            return "{\"serializeError\":\"" + exception.getMessage() + "\"}";
        }
    }

    private String classifyFailure(RuntimeException failure) {
        if (failure == null) {
            return "UNKNOWN";
        }
        return failure instanceof com.fenxiao.common.api.ForbiddenException ? "REJECTED" : "FAILED";
    }

    private String buildSourceEventId(String linkyOrderId) {
        return "LINKY:" + linkyOrderId.trim();
    }

    private String trimMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.length() > 255 ? message.substring(0, 255) : message;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
