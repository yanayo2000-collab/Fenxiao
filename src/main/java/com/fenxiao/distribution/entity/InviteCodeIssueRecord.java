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
@Table(name = "invite_code_issue_record")
public class InviteCodeIssueRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issuer_user_id", nullable = false, unique = true)
    private Long issuerUserId;

    @Column(name = "product_code", nullable = false, length = 32)
    private String productCode;

    @Column(name = "whatsapp_number", nullable = false, unique = true, length = 32)
    private String whatsappNumber;

    @Column(name = "app_account", nullable = false, unique = true, length = 16)
    private String appAccount;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    protected InviteCodeIssueRecord() {
    }

    public Long getId() {
        return id;
    }

    public Long getIssuerUserId() {
        return issuerUserId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getAppAccount() {
        return appAccount;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public static InviteCodeIssueRecord create(Long issuerUserId,
                                               String productCode,
                                               String whatsappNumber,
                                               String appAccount,
                                               String inviteCode) {
        InviteCodeIssueRecord record = new InviteCodeIssueRecord();
        record.issuerUserId = issuerUserId;
        record.productCode = productCode;
        record.whatsappNumber = whatsappNumber;
        record.appAccount = appAccount;
        record.inviteCode = inviteCode;
        record.issuedAt = LocalDateTime.now(Clock.systemUTC());
        return record;
    }
}