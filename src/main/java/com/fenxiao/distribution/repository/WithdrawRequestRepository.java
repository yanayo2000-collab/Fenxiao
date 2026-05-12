package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    boolean existsByUserIdAndRequestWeek(Long userId, String requestWeek);

    Optional<WithdrawRequest> findByRequestNo(String requestNo);

    @Query("""
            select r from WithdrawRequest r
            where (:userId is null or r.userId = :userId)
              and (:status is null or r.requestStatus = :status)
            order by r.id desc
            """)
    Page<WithdrawRequest> findAdminRequests(Long userId, String status, Pageable pageable);
}
