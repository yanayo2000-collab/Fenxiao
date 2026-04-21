package com.fenxiao.reward.repository;

import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RewardRecordRepository extends JpaRepository<RewardRecord, Long> {
    List<RewardRecord> findBySourceEventIdOrderByRewardLevelAsc(String sourceEventId);
    Optional<RewardRecord> findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(String sourceEventId, Long beneficiaryUserId, Integer rewardLevel);
    List<RewardRecord> findTop50ByOrderByIdDesc();
    List<RewardRecord> findByBeneficiaryUserIdOrderByIdDesc(Long beneficiaryUserId);
    List<RewardRecord> findByBeneficiaryUserIdAndRewardStatusOrderByIdDesc(Long beneficiaryUserId, RewardStatus rewardStatus);
    List<RewardRecord> findByRewardStatusOrderByIdDesc(RewardStatus rewardStatus);
    List<RewardRecord> findByRewardStatusAndUnfreezeAtLessThanEqual(RewardStatus rewardStatus, LocalDateTime unfreezeAt);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r")
    BigDecimal sumRewardAmount();

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.rewardStatus = :status")
    BigDecimal sumRewardAmountByStatus(RewardStatus status);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.beneficiaryUserId = :beneficiaryUserId")
    BigDecimal sumRewardAmountByBeneficiaryUserId(Long beneficiaryUserId);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.beneficiaryUserId = :beneficiaryUserId and r.rewardStatus = :status")
    BigDecimal sumRewardAmountByBeneficiaryUserIdAndStatus(Long beneficiaryUserId, RewardStatus status);
}
