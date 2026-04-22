package com.fenxiao.linky.repository;

import com.fenxiao.linky.entity.LinkyReplayRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LinkyReplayRecordRepository extends JpaRepository<LinkyReplayRecord, Long> {

    Optional<LinkyReplayRecord> findByRequestFingerprint(String requestFingerprint);

    @Query("""
            select r from LinkyReplayRecord r
            where (:linkyOrderId is null or r.linkyOrderId = :linkyOrderId)
              and (:userId is null or r.userId = :userId)
            order by r.lastSeenAt desc, r.id desc
            """)
    Page<LinkyReplayRecord> findForAdmin(@Param("linkyOrderId") String linkyOrderId,
                                         @Param("userId") Long userId,
                                         Pageable pageable);
}
