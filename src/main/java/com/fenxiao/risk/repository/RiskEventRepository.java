package com.fenxiao.risk.repository;

import com.fenxiao.risk.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {
}
