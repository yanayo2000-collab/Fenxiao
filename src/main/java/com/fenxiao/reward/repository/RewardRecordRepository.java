package com.fenxiao.reward.repository;

import com.fenxiao.reward.entity.RewardRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardRecordRepository extends JpaRepository<RewardRecord, Long> {
    List<RewardRecord> findBySourceEventIdOrderByRewardLevelAsc(String sourceEventId);
    Optional<RewardRecord> findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(String sourceEventId, Long beneficiaryUserId, Integer rewardLevel);
    List<RewardRecord> findTop50ByOrderByIdDesc();
}
