package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.InviteCodeIssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InviteCodeIssueRecordRepository extends JpaRepository<InviteCodeIssueRecord, Long> {
    Optional<InviteCodeIssueRecord> findByWhatsappNumber(String whatsappNumber);

    Optional<InviteCodeIssueRecord> findByAppAccount(String appAccount);

    Optional<InviteCodeIssueRecord> findByIssuerUserId(Long issuerUserId);

    @Query("select r.issuerUserId from InviteCodeIssueRecord r where r.productCode = :productCode order by r.issuerUserId asc")
    List<Long> findIssuerUserIdsByProductCode(String productCode);
}
