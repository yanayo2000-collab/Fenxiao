package com.fenxiao.admin.repository;

import com.fenxiao.admin.entity.AdminSecurityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminSecurityEventRepository extends JpaRepository<AdminSecurityEvent,Long>{
    List<AdminSecurityEvent> findByAccountIdOrderByOccurredAtDesc(Long accountId,Pageable pageable);
    boolean existsByAccountIdAndIpAddressAndSuccessTrue(Long accountId,String ipAddress);
}
