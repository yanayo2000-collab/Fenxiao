package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_product_ownership")
public class UserProductOwnership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "ownership_status", nullable = false, length = 32)
    private String ownershipStatus;

    @Column(name = "ownership_source", nullable = false, length = 32)
    private String ownershipSource;

    @Column(name = "source_record_type", length = 64)
    private String sourceRecordType;

    @Column(name = "source_record_id")
    private Long sourceRecordId;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    protected UserProductOwnership() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getOwnershipStatus() {
        return ownershipStatus;
    }

    public String getOwnershipSource() {
        return ownershipSource;
    }

    public String getSourceRecordType() {
        return sourceRecordType;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public void activate(String ownershipSource, String sourceRecordType, Long sourceRecordId) {
        this.ownershipStatus = "ACTIVE";
        this.ownershipSource = ownershipSource;
        this.sourceRecordType = sourceRecordType;
        this.sourceRecordId = sourceRecordId;
        this.effectiveAt = LocalDateTime.now(Clock.systemUTC());
    }

    public void markCorrected() {
        this.ownershipStatus = "CORRECTED";
        this.effectiveAt = LocalDateTime.now(Clock.systemUTC());
    }

    public static UserProductOwnership create(Long userId,
                                              String productCode,
                                              String ownershipSource,
                                              String sourceRecordType,
                                              Long sourceRecordId) {
        UserProductOwnership ownership = new UserProductOwnership();
        ownership.userId = userId;
        ownership.productCode = productCode;
        ownership.ownershipStatus = "ACTIVE";
        ownership.ownershipSource = ownershipSource;
        ownership.sourceRecordType = sourceRecordType;
        ownership.sourceRecordId = sourceRecordId;
        ownership.effectiveAt = LocalDateTime.now(Clock.systemUTC());
        return ownership;
    }
}