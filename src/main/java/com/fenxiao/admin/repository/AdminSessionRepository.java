package com.fenxiao.admin.repository;

import com.fenxiao.admin.entity.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSession,Long>{
    Optional<AdminSession> findByTokenHash(String tokenHash);
    List<AdminSession> findByAccountIdAndRevokedAtIsNull(Long accountId);
}
