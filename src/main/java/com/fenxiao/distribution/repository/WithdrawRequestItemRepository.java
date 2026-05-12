package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.WithdrawRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawRequestItemRepository extends JpaRepository<WithdrawRequestItem, Long> {
    List<WithdrawRequestItem> findByWithdrawRequestId(Long withdrawRequestId);
}
