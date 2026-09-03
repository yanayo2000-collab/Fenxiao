package com.fenxiao.admin.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_session")
public class AdminSession extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="account_id",nullable=false) private Long accountId;
    @Column(name="token_hash",nullable=false,length=64,unique=true) private String tokenHash;
    @Column(name="remember_me",nullable=false) private boolean rememberMe;
    @Column(name="issued_at",nullable=false) private LocalDateTime issuedAt;
    @Column(name="last_seen_at",nullable=false) private LocalDateTime lastSeenAt;
    @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
    @Column(name="revoked_at") private LocalDateTime revokedAt;
    @Column(name="revoke_reason",length=128) private String revokeReason;
    @Column(name="ip_address",length=64) private String ipAddress;
    @Column(name="user_agent",length=512) private String userAgent;
    protected AdminSession() {}
    public static AdminSession issue(Long accountId,String tokenHash,boolean rememberMe,LocalDateTime now,LocalDateTime expiresAt,String ip,String userAgent){
        var value=new AdminSession(); value.accountId=accountId; value.tokenHash=tokenHash; value.rememberMe=rememberMe;
        value.issuedAt=now; value.lastSeenAt=now; value.expiresAt=expiresAt; value.ipAddress=ip; value.userAgent=userAgent; return value;
    }
    public Long getId(){return id;} public Long getAccountId(){return accountId;} public String getTokenHash(){return tokenHash;}
    public boolean isRememberMe(){return rememberMe;} public LocalDateTime getIssuedAt(){return issuedAt;} public LocalDateTime getLastSeenAt(){return lastSeenAt;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public LocalDateTime getRevokedAt(){return revokedAt;} public String getRevokeReason(){return revokeReason;}
    public String getIpAddress(){return ipAddress;} public String getUserAgent(){return userAgent;}
    public boolean isUsableAt(LocalDateTime now){return revokedAt==null && expiresAt.isAfter(now);}
    public void touch(LocalDateTime now,long idleDays){lastSeenAt=now;if(rememberMe)expiresAt=now.plusDays(idleDays);}
    public void revoke(LocalDateTime now,String reason){if(revokedAt==null){revokedAt=now;revokeReason=reason;}}
}
