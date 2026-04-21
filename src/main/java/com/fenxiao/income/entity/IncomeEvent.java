package com.fenxiao.income.entity;

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
@Table(name = "income_event")
public class IncomeEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_event_id", nullable = false, unique = true, length = 64)
    private String sourceEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "income_type", nullable = false, length = 32)
    private String incomeType;

    @Column(name = "income_amount", nullable = false, precision = 18, scale = 6)
    private BigDecimal incomeAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "sync_batch_no", length = 64)
    private String syncBatchNo;

    @Column(name = "sync_status", nullable = false, length = 32)
    private String syncStatus;

    @Lob
    @Column(name = "raw_payload")
    private String rawPayload;

    protected IncomeEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getIncomeType() {
        return incomeType;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getSyncBatchNo() {
        return syncBatchNo;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public static IncomeEvent create(String sourceEventId,
                                     Long userId,
                                     String countryCode,
                                     BigDecimal incomeAmount,
                                     String currencyCode,
                                     LocalDateTime eventTime) {
        IncomeEvent event = new IncomeEvent();
        event.sourceEventId = sourceEventId;
        event.userId = userId;
        event.countryCode = countryCode;
        event.incomeType = "CONFIRMED";
        event.incomeAmount = incomeAmount;
        event.currencyCode = currencyCode;
        event.eventTime = eventTime;
        event.syncStatus = "PROCESSED";
        return event;
    }
}
