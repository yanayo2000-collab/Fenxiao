package com.fenxiao.income.repository;

import com.fenxiao.income.entity.IncomeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncomeEventRepository extends JpaRepository<IncomeEvent, Long> {
    Optional<IncomeEvent> findBySourceEventId(String sourceEventId);
}
