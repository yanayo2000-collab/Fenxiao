package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.InviteCodeIssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeIssueRecordRepository extends JpaRepository<InviteCodeIssueRecord, Long> {
    Optional<InviteCodeIssueRecord> findByWhatsappNumber(String whatsappNumber);

    Optional<InviteCodeIssueRecord> findByAppAccount(String appAccount);

    Optional<InviteCodeIssueRecord> findByIssuerUserId(Long issuerUserId);
}