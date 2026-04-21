package com.fenxiao.audit.repository;

import com.fenxiao.audit.entity.OperationAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationAuditLogRepository extends JpaRepository<OperationAuditLog, Long> {

    @Query("""
            select l from OperationAuditLog l
            where (:moduleName is null or l.moduleName = :moduleName)
            order by l.operatedAt desc, l.id desc
            """)
    Page<OperationAuditLog> findAdminAuditLogs(String moduleName, Pageable pageable);
}
