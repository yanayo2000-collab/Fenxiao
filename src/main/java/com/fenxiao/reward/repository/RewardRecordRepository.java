package com.fenxiao.reward.repository;

import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RewardRecordRepository extends JpaRepository<RewardRecord, Long> {
    List<RewardRecord> findBySourceEventIdOrderByRewardLevelAsc(String sourceEventId);
    Optional<RewardRecord> findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(String sourceEventId, Long beneficiaryUserId, Integer rewardLevel);
    List<RewardRecord> findTop50ByOrderByIdDesc();
    List<RewardRecord> findByBeneficiaryUserIdOrderByIdDesc(Long beneficiaryUserId);
    List<RewardRecord> findByBeneficiaryUserIdAndRewardStatusOrderByIdDesc(Long beneficiaryUserId, RewardStatus rewardStatus);
    List<RewardRecord> findByBeneficiaryUserIdAndRewardStatus(Long beneficiaryUserId, RewardStatus rewardStatus);
    List<RewardRecord> findByBeneficiaryUserIdAndRewardStatusIn(Long beneficiaryUserId, Collection<RewardStatus> rewardStatuses);
    List<RewardRecord> findBySourceUserIdAndRewardStatus(Long sourceUserId, RewardStatus rewardStatus);
    List<RewardRecord> findBySourceUserIdAndRewardStatusIn(Long sourceUserId, Collection<RewardStatus> rewardStatuses);
    List<RewardRecord> findByRewardStatusOrderByIdDesc(RewardStatus rewardStatus);
    List<RewardRecord> findByRewardStatusAndUnfreezeAtLessThanEqual(RewardStatus rewardStatus, LocalDateTime unfreezeAt);

    @Query("""
            select r from RewardRecord r
            where (:beneficiaryUserId is null or r.beneficiaryUserId = :beneficiaryUserId)
              and (:status is null or r.rewardStatus = :status)
              and (:startAt is null or r.calculatedAt >= :startAt)
              and (:endAt is null or r.calculatedAt <= :endAt)
            order by r.id desc
            """)
    Page<RewardRecord> findAdminRewards(Long beneficiaryUserId,
                                        RewardStatus status,
                                        LocalDateTime startAt,
                                        LocalDateTime endAt,
                                        Pageable pageable);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r")
    BigDecimal sumRewardAmount();

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.rewardStatus = :status")
    BigDecimal sumRewardAmountByStatus(RewardStatus status);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.beneficiaryUserId = :beneficiaryUserId")
    BigDecimal sumRewardAmountByBeneficiaryUserId(Long beneficiaryUserId);

    @Query("select coalesce(sum(r.rewardAmount), 0) from RewardRecord r where r.beneficiaryUserId = :beneficiaryUserId and r.rewardStatus = :status")
    BigDecimal sumRewardAmountByBeneficiaryUserIdAndStatus(Long beneficiaryUserId, RewardStatus status);
}
