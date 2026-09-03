package com.fenxiao.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="admin_password_history")
public class AdminPasswordHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="account_id",nullable=false) private Long accountId;
    @Column(name="password_hash",nullable=false,length=256) private String passwordHash;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    protected AdminPasswordHistory(){}
    public static AdminPasswordHistory create(Long accountId,String hash,LocalDateTime at){var v=new AdminPasswordHistory();v.accountId=accountId;v.passwordHash=hash;v.createdAt=at;return v;}
    public String getPasswordHash(){return passwordHash;}
}
