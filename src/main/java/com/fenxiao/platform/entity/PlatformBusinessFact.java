package com.fenxiao.platform.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.platform.domain.PlatformFactType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_business_fact")
public class PlatformBusinessFact extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_event_id", nullable = false, length = 128) private String sourceEventId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "platform_code", nullable = false, length = 32) private String platformCode;
    @Column(name = "platform_user_id", nullable = false, length = 64) private String platformUserId;
    @Enumerated(EnumType.STRING) @Column(name = "fact_type", nullable = false, length = 64) private PlatformFactType factType;
    @Column(name = "amount", precision = 18, scale = 6) private BigDecimal amount;
    @Column(name = "currency_code", length = 16) private String currencyCode;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "business_date", nullable = false) private LocalDate businessDate;
    @Column(name = "guild_id", length = 64) private String guildId;
    @Column(name = "source_system", nullable = false, length = 64) private String sourceSystem;
    @Column(name = "source_version", length = 64) private String sourceVersion;
    @Column(name = "payload_hash", length = 128) private String payloadHash;
    @Column(name = "received_at", nullable = false) private LocalDateTime receivedAt;

    protected PlatformBusinessFact() {}

    public static PlatformBusinessFact create(String sourceEventId, PlatformAccountBinding binding, PlatformFactType type,
                                              BigDecimal amount, String currencyCode, LocalDateTime occurredAt,
                                              String guildId, String sourceSystem, String sourceVersion,
                                              String payloadHash, LocalDateTime receivedAt) {
        PlatformBusinessFact value = new PlatformBusinessFact();
        value.sourceEventId = sourceEventId.trim();
        value.userId = binding.getUserId();
        value.platformCode = binding.getPlatformCode();
        value.platformUserId = binding.getPlatformUserId();
        value.factType = type;
        value.amount = amount;
        value.currencyCode = currencyCode == null ? null : currencyCode.trim().toUpperCase();
        value.occurredAt = occurredAt;
        value.businessDate = occurredAt.toLocalDate();
        value.guildId = guildId;
        value.sourceSystem = sourceSystem.trim();
        value.sourceVersion = sourceVersion;
        value.payloadHash = payloadHash;
        value.receivedAt = receivedAt;
        return value;
    }

    public PlatformFactType getFactType() { return factType; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public LocalDate getBusinessDate() { return businessDate; }
}

