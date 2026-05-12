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

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
