package com.fenxiao.distribution.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Clock;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verification_code")
public class PhoneVerificationCode extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;
    @Column(name = "verification_code", nullable = false, length = 16)
    private String verificationCode;
    @Column(name = "purpose", nullable = false, length = 32)
    private String purpose;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "attempts", nullable = false)
    private int attempts;
    @Column(name = "consumed", nullable = false)
    private boolean consumed;

    protected PhoneVerificationCode() {}
    public Long getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getVerificationCode() { return verificationCode; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getAttempts() { return attempts; }
    public boolean isConsumed() { return consumed; }

    public static PhoneVerificationCode issue(String phoneNumber, String code, String purpose, LocalDateTime expiresAt) {
        PhoneVerificationCode v = new PhoneVerificationCode();
        v.phoneNumber = phoneNumber;
        v.verificationCode = code;
        v.purpose = purpose;
        v.expiresAt = expiresAt;
        v.attempts = 0;
        v.consumed = false;
        return v;
    }
    public void failAttempt() { this.attempts++; }
    public void consume() { this.consumed = true; }
    public boolean expired(Clock clock) { return !expiresAt.isAfter(LocalDateTime.now(clock)); }
}
