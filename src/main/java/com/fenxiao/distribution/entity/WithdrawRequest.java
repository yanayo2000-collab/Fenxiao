package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdraw_request")
public class WithdrawRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_no", nullable = false, unique = true, length = 64)
    private String requestNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "requested_diamond_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal requestedDiamondAmount;

    @Column(name = "approved_diamond_amount", precision = 18, scale = 6)
    private BigDecimal approvedDiamondAmount;

    @Column(name = "request_status", nullable = false, length = 32)
    private String requestStatus;

    @Column(name = "request_week", nullable = false, length = 20)
    private String requestWeek;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "payment_channel", length = 32)
    private String paymentChannel;
    @Column(name = "payment_reference", length = 128)
    private String paymentReference;
    @Column(name = "payment_evidence_uri", length = 255)
    private String paymentEvidenceUri;
    @Column(name = "payment_evidence_hash", length = 128)
    private String paymentEvidenceHash;
    @Column(name = "paid_by")
    private Long paidBy;
    @Column(name = "payment_failure_reason", length = 255)
    private String paymentFailureReason;
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
    @Column(name = "reversed_by")
    private Long reversedBy;
    @Column(name = "reversal_reason", length = 255)
    private String reversalReason;

    protected WithdrawRequest() {
    }

    public Long getId() {
        return id;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getRequestedDiamondAmount() {
        return requestedDiamondAmount;
    }

    public BigDecimal getApprovedDiamondAmount() {
        return approvedDiamondAmount;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public String getRequestWeek() {
        return requestWeek;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getRemark() {
        return remark;
    }

    public String getPaymentChannel() { return paymentChannel; }
    public String getPaymentReference() { return paymentReference; }
    public String getPaymentEvidenceUri() { return paymentEvidenceUri; }
    public String getPaymentEvidenceHash() { return paymentEvidenceHash; }
    public Long getPaidBy() { return paidBy; }
    public String getPaymentFailureReason() { return paymentFailureReason; }
    public LocalDateTime getReversedAt() { return reversedAt; }
    public Long getReversedBy() { return reversedBy; }
    public String getReversalReason() { return reversalReason; }

    public static WithdrawRequest create(Long userId, BigDecimal requestedDiamondAmount, String requestWeek) {
        WithdrawRequest request = new WithdrawRequest();
        request.requestNo = "WD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        request.userId = userId;
        request.requestedDiamondAmount = requestedDiamondAmount;
        request.requestStatus = "PENDING_REVIEW";
        request.requestWeek = requestWeek;
        request.requestedAt = LocalDateTime.now(Clock.systemUTC());
        return request;
    }

    public void markPaidOut(Long reviewerId, String remark, LocalDateTime now) {
        assertPendingReview();
        this.requestStatus = "PAID_OUT";
        this.approvedDiamondAmount = this.requestedDiamondAmount;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.paidAt = now;
        this.rejectReason = null;
        this.remark = normalizeText(remark);
        this.paymentChannel = "MANUAL";
        this.paidBy = reviewerId;
    }

    public void approveForPayment(Long reviewerId, String remark, LocalDateTime now) {
        assertPendingReview();
        this.requestStatus = "PAYMENT_PENDING";
        this.approvedDiamondAmount = this.requestedDiamondAmount;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.rejectReason = null;
        this.remark = normalizeText(remark);
    }

    public void markPaymentSucceeded(Long operatorId, String channel, String reference, String evidenceUri, String evidenceHash, LocalDateTime now) {
        assertStatus("PAYMENT_PENDING", "PAYMENT_FAILED");
        this.requestStatus = "PAID_OUT";
        this.paidAt = now;
        this.paidBy = operatorId;
        this.paymentChannel = required(channel, "payment channel");
        this.paymentReference = normalizeText(reference);
        this.paymentEvidenceUri = normalizeText(evidenceUri);
        this.paymentEvidenceHash = normalizeText(evidenceHash);
        this.paymentFailureReason = null;
    }

    public void markPaymentFailed(String failureReason) {
        assertStatus("PAYMENT_PENDING");
        this.requestStatus = "PAYMENT_FAILED";
        this.paymentFailureReason = required(failureReason, "payment failure reason");
    }

    public void markReversed(Long operatorId, String reason, LocalDateTime now) {
        assertStatus("PAID_OUT");
        this.requestStatus = "REVERSED";
        this.reversedAt = now;
        this.reversedBy = operatorId;
        this.reversalReason = required(reason, "reversal reason");
    }

    public void markRejected(Long reviewerId, String reason, LocalDateTime now) {
        assertPendingReview();
        this.requestStatus = "REJECTED";
        this.approvedDiamondAmount = BigDecimal.ZERO;
        this.reviewedBy = reviewerId;
        this.reviewedAt = now;
        this.paidAt = null;
        this.rejectReason = normalizeText(reason);
        this.remark = normalizeText(reason);
    }

    private void assertPendingReview() {
        if (!"PENDING_REVIEW".equals(this.requestStatus)) {
            throw new IllegalStateException("withdraw request already reviewed");
        }
    }

    private void assertStatus(String... allowed) {
        for (String status : allowed) if (status.equals(this.requestStatus)) return;
        throw new IllegalStateException("withdraw request transition is not allowed from " + this.requestStatus);
    }

    private String required(String value, String label) {
        String normalized = normalizeText(value);
        if (normalized == null) throw new IllegalArgumentException(label + " is required");
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
