package com.fenxiao.linky.repository;

import com.fenxiao.linky.entity.LinkyWebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkyWebhookLogRepository extends JpaRepository<LinkyWebhookLog, Long> {

    @Query("""
            select l from LinkyWebhookLog l
            where (:linkyOrderId is null or l.linkyOrderId = :linkyOrderId)
              and (:userId is null or l.userId = :userId)
              and (:requestStatus is null or l.requestStatus = :requestStatus)
            order by l.requestReceivedAt desc, l.id desc
            """)
    Page<LinkyWebhookLog> findForAdmin(@Param("linkyOrderId") String linkyOrderId,
                                       @Param("userId") Long userId,
                                       @Param("requestStatus") String requestStatus,
                                       Pageable pageable);
}
