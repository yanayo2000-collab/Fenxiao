package com.fenxiao.admin.repository;

import com.fenxiao.admin.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {
    Optional<AdminAccount> findByUsername(String username);
    long countByRoleIgnoreCaseAndEnabledTrue(String role);
}
