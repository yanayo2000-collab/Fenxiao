package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "invite_binding_registration")
public class InviteBindingRegistration extends BaseEntity {

    @Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter_user_id", nullable = false)
    private Long inviterUserId;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(name = "whatsapp_number", nullable = false, unique = true, length = 32)
    private String whatsappNumber;

    @Column(name = "linky_account", nullable = false, unique = true, length = 16)
    private String linkyAccount;

    @Column(name = "bind_status", nullable = false, length = 32)
    private String bindStatus;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "remark", length = 255)
    private String remark;

    protected InviteBindingRegistration() {
    }

    public Long getId() {
        return id;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getLinkyAccount() {
        return linkyAccount;
    }

    public String getBindStatus() {
        return bindStatus;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getRemark() {
        return remark;
    }

    public static InviteBindingRegistration createActive(Long inviterUserId,
                                                         String inviteCode,
                                                         String whatsappNumber,
                                                         String linkyAccount) {
        InviteBindingRegistration registration = new InviteBindingRegistration();
        registration.inviterUserId = inviterUserId;
        registration.inviteCode = inviteCode;
        registration.whatsappNumber = whatsappNumber;
        registration.linkyAccount = linkyAccount;
        registration.bindStatus = "ACTIVE";
        registration.submittedAt = LocalDateTime.now(Clock.systemUTC());
        return registration;
    }
}
