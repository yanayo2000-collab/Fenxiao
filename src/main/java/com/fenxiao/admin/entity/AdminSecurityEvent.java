package com.fenxiao.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="admin_security_event")
public class AdminSecurityEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="account_id") private Long accountId;
    @Column(name="username",length=64) private String username;
    @Column(name="event_type",nullable=false,length=64) private String eventType;
    @Column(name="success",nullable=false) private boolean success;
    @Column(name="ip_address",length=64) private String ipAddress;
    @Column(name="user_agent",length=512) private String userAgent;
    @Column(name="detail",length=512) private String detail;
    @Column(name="occurred_at",nullable=false) private LocalDateTime occurredAt;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    protected AdminSecurityEvent(){}
    public static AdminSecurityEvent create(Long accountId,String username,String type,boolean success,String ip,String userAgent,String detail,LocalDateTime at){var v=new AdminSecurityEvent();v.accountId=accountId;v.username=username;v.eventType=type;v.success=success;v.ipAddress=ip;v.userAgent=userAgent;v.detail=detail;v.occurredAt=at;v.createdAt=at;return v;}
    public Long getId(){return id;} public Long getAccountId(){return accountId;} public String getUsername(){return username;} public String getEventType(){return eventType;}
    public boolean isSuccess(){return success;} public String getIpAddress(){return ipAddress;} public String getUserAgent(){return userAgent;} public String getDetail(){return detail;} public LocalDateTime getOccurredAt(){return occurredAt;}
}
