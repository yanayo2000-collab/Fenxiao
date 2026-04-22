package com.fenxiao.linky.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "linky_webhook_log")
public class LinkyWebhookLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linky_order_id", nullable = false, length = 80)
    private String linkyOrderId;

    @Column(name = "source_event_id", length = 80)
    private String sourceEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "income_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal incomeAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "request_received_at", nullable = false)
    private LocalDateTime requestReceivedAt;

    @Column(name = "linky_timestamp", length = 64)
    private String linkyTimestamp;

    @Column(name = "linky_signature", length = 255)
    private String linkySignature;

    @Column(name = "internal_token_status", nullable = false, length = 32)
    private String internalTokenStatus;

    @Column(name = "signature_status", nullable = false, length = 32)
    private String signatureStatus;

    @Column(name = "replay_status", nullable = false, length = 32)
    private String replayStatus;

    @Column(name = "replay_record_status", nullable = false, length = 32)
    private String replayRecordStatus;

    @Column(name = "request_fingerprint", length = 128)
    private String requestFingerprint;

    @Column(name = "replay_hit_count")
    private Integer replayHitCount;

    @Column(name = "request_status", nullable = false, length = 32)
    private String requestStatus;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "request_ip", length = 64)
    private String requestIp;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    protected LinkyWebhookLog() {
    }

    public Long getId() {
        return id;
    }

    public String getLinkyOrderId() {
        return linkyOrderId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getRequestReceivedAt() {
        return requestReceivedAt;
    }

    public String getLinkyTimestamp() {
        return linkyTimestamp;
    }

    public String getLinkySignature() {
        return linkySignature;
    }

    public String getInternalTokenStatus() {
        return internalTokenStatus;
    }

    public String getSignatureStatus() {
        return signatureStatus;
    }

    public String getReplayStatus() {
        return replayStatus;
    }

    public String getReplayRecordStatus() {
        return replayRecordStatus;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public Integer getReplayHitCount() {
        return replayHitCount;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public static LinkyWebhookLog create(String linkyOrderId,
                                         String sourceEventId,
                                         Long userId,
                                         BigDecimal incomeAmount,
                                         String currencyCode,
                                         LocalDateTime paidAt,
                                         LocalDateTime requestReceivedAt,
                                         String linkyTimestamp,
                                         String linkySignature,
                                         String internalTokenStatus,
                                         String signatureStatus,
                                         String replayStatus,
                                         String replayRecordStatus,
                                         String requestFingerprint,
                                         Integer replayHitCount,
                                         String requestStatus,
                                         String failureReason,
                                         String requestIp,
                                         String payloadJson) {
        LinkyWebhookLog log = new LinkyWebhookLog();
        log.linkyOrderId = linkyOrderId;
        log.sourceEventId = sourceEventId;
        log.userId = userId;
        log.incomeAmount = incomeAmount;
        log.currencyCode = currencyCode;
        log.paidAt = paidAt;
        log.requestReceivedAt = requestReceivedAt;
        log.linkyTimestamp = linkyTimestamp;
        log.linkySignature = linkySignature;
        log.internalTokenStatus = internalTokenStatus;
        log.signatureStatus = signatureStatus;
        log.replayStatus = replayStatus;
        log.replayRecordStatus = replayRecordStatus;
        log.requestFingerprint = requestFingerprint;
        log.replayHitCount = replayHitCount;
        log.requestStatus = requestStatus;
        log.failureReason = failureReason;
        log.requestIp = requestIp;
        log.payloadJson = payloadJson;
        return log;
    }
}
