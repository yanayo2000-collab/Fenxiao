package com.fenxiao.income.repository;

import com.fenxiao.income.entity.IncomeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface IncomeEventRepository extends JpaRepository<IncomeEvent, Long> {
    Optional<IncomeEvent> findBySourceEventId(String sourceEventId);

    long countByRewardEngineVersion(String rewardEngineVersion);

    @Query("select coalesce(sum(i.legacyCandidateCount), 0) from IncomeEvent i where i.rewardEngineVersion = :rewardEngineVersion")
    long sumLegacyCandidateCountByRewardEngineVersion(String rewardEngineVersion);

    @Query("select coalesce(sum(i.generatedRewardCount), 0) from IncomeEvent i where i.rewardEngineVersion = :rewardEngineVersion")
    long sumGeneratedRewardCountByRewardEngineVersion(String rewardEngineVersion);

    @Query("select coalesce(sum(i.incomeAmount), 0) from IncomeEvent i where i.userId = :userId and i.eventTime >= :startAt and i.eventTime < :endAt")
    BigDecimal sumIncomeAmountByUserIdAndEventTimeBetween(Long userId, LocalDateTime startAt, LocalDateTime endAt);

    @Query("select coalesce(sum(i.incomeAmount), 0) from IncomeEvent i where i.userId in :userIds and i.eventTime >= :startAt and i.eventTime < :endAt")
    BigDecimal sumIncomeAmountByUserIdInAndEventTimeBetween(Collection<Long> userIds, LocalDateTime startAt, LocalDateTime endAt);
}
