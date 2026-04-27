package com.fenxiao.risk.repository;

import com.fenxiao.risk.domain.RiskStatus;
import com.fenxiao.risk.entity.RiskEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    @Query("""
            select r from RiskEvent r
            where (:userId is null or r.userId = :userId)
              and (:riskStatus is null or r.riskStatus = :riskStatus)
              and (:startAt is null or r.detectedAt >= :startAt)
              and (:endAt is null or r.detectedAt <= :endAt)
            order by r.id desc
            """)
    Page<RiskEvent> findAdminRiskEvents(Long userId,
                                        RiskStatus riskStatus,
                                        LocalDateTime startAt,
                                        LocalDateTime endAt,
                                        Pageable pageable);

    @Query("""
            select r from RiskEvent r
            where r.userId in :userIds
              and (:riskStatus is null or r.riskStatus = :riskStatus)
              and (:startAt is null or r.detectedAt >= :startAt)
              and (:endAt is null or r.detectedAt <= :endAt)
            order by r.id desc
            """)
    Page<RiskEvent> findAdminRiskEventsByUserIdIn(Collection<Long> userIds,
                                                  RiskStatus riskStatus,
                                                  LocalDateTime startAt,
                                                  LocalDateTime endAt,
                                                  Pageable pageable);

    long countByUserIdIn(Collection<Long> userIds);
}
