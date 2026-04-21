package com.fenxiao.reward.repository;

import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RewardRecordRepository extends JpaRepository<RewardRecord, Long> {
    List<RewardRecord> findBySourceEventIdOrderByRewardLevelAsc(String sourceEventId);
    Optional<RewardRecord> findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(String sourceEventId, Long beneficiaryUserId, Integer rewardLevel);
    List<RewardRecord> findTop50ByOrderByIdDesc();
    List<RewardRecord> findByBeneficiaryUserIdOrderByIdDesc(Long beneficiaryUserId);
    List<RewardRecord> findByRewardStatusOrderByIdDesc(RewardStatus rewardStatus);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r")
    BigDecimal sumRewardAmount();

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.rewardStatus = :status")
    BigDecimal sumRewardAmountByStatus(RewardStatus status);
}
