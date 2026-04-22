package com.fenxiao.linky.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "linky_replay_record")
public class LinkyReplayRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_fingerprint", nullable = false, unique = true, length = 128)
    private String requestFingerprint;

    @Column(name = "linky_order_id", nullable = false, length = 80)
    private String linkyOrderId;

    @Column(name = "source_event_id", nullable = false, length = 80)
    private String sourceEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount;

    @Column(name = "latest_request_status", nullable = false, length = 32)
    private String latestRequestStatus;

    @Column(name = "latest_failure_reason", length = 255)
    private String latestFailureReason;

    protected LinkyReplayRecord() {
    }

    public Long getId() {
        return id;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
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

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public String getLatestRequestStatus() {
        return latestRequestStatus;
    }

    public String getLatestFailureReason() {
        return latestFailureReason;
    }

    public static LinkyReplayRecord create(String requestFingerprint,
                                           String linkyOrderId,
                                           String sourceEventId,
                                           Long userId,
                                           LocalDateTime firstSeenAt,
                                           String latestRequestStatus,
                                           String latestFailureReason) {
        LinkyReplayRecord record = new LinkyReplayRecord();
        record.requestFingerprint = requestFingerprint;
        record.linkyOrderId = linkyOrderId;
        record.sourceEventId = sourceEventId;
        record.userId = userId;
        record.firstSeenAt = firstSeenAt;
        record.lastSeenAt = firstSeenAt;
        record.hitCount = 1;
        record.latestRequestStatus = latestRequestStatus;
        record.latestFailureReason = latestFailureReason;
        return record;
    }

    public void markSeenAgain(LocalDateTime seenAt, String latestRequestStatus, String latestFailureReason) {
        this.lastSeenAt = seenAt;
        this.hitCount = this.hitCount + 1;
        this.latestRequestStatus = latestRequestStatus;
        this.latestFailureReason = latestFailureReason;
    }
}
