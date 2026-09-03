package com.fenxiao.admin.repository;

import com.fenxiao.admin.entity.AdminPasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminPasswordHistoryRepository extends JpaRepository<AdminPasswordHistory,Long>{
    List<AdminPasswordHistory> findTop5ByAccountIdOrderByCreatedAtDesc(Long accountId);
}
