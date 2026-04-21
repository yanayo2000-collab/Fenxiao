package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.DistributionHomeResponse;
import com.fenxiao.distribution.api.dto.TeamListResponse;
import com.fenxiao.distribution.api.dto.TeamMemberItem;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.reward.api.dto.RewardListItem;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DistributionFrontendService {

    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final DistributionRelationRepository distributionRelationRepository;
    private final RewardRecordRepository rewardRecordRepository;

    public DistributionFrontendService(UserDistributionProfileRepository userDistributionProfileRepository,
                                       DistributionRelationRepository distributionRelationRepository,
                                       RewardRecordRepository rewardRecordRepository) {
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.distributionRelationRepository = distributionRelationRepository;
        this.rewardRecordRepository = rewardRecordRepository;
    }

    public DistributionHomeResponse getHome(Long userId) {
        UserDistributionProfile profile = userDistributionProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));
        DistributionRelation relation = distributionRelationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));
        List<DistributionRelation> directRelations = distributionRelationRepository.findByLevel1InviterIdOrderByIdDesc(userId);

        long effectiveUsers = resolveEffectiveUsers(directRelations);
        BigDecimal totalReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserId(userId);
        BigDecimal frozenReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndStatus(userId, RewardStatus.FROZEN);
        BigDecimal availableReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndStatus(userId, RewardStatus.AVAILABLE);
        BigDecimal riskHoldReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndStatus(userId, RewardStatus.RISK_HOLD);

        return new DistributionHomeResponse(
                userId,
                profile.getInviteCode(),
                relation.getLevel1InviterId(),
                directRelations.size(),
                effectiveUsers,
                totalReward,
                frozenReward,
                availableReward,
                riskHoldReward
        );
    }

    public TeamListResponse getDirectTeam(Long userId) {
        List<DistributionRelation> directRelations = distributionRelationRepository.findByLevel1InviterIdOrderByIdDesc(userId);
        Map<Long, UserDistributionProfile> profileMap = loadProfileMap(directRelations);
        List<TeamMemberItem> items = new ArrayList<>();
        for (DistributionRelation relation : directRelations) {
            UserDistributionProfile profile = profileMap.get(relation.getUserId());
            if (profile == null) {
                continue;
            }
            items.add(new TeamMemberItem(
                    profile.getUserId(),
                    profile.getInviteCode(),
                    profile.getCountryCode(),
                    profile.isEffectiveUser(),
                    profile.getConfirmedIncomeTotal(),
                    relation.getLockStatus(),
                    relation.getBindTime()
            ));
        }
        return new TeamListResponse(items, items.size());
    }

    public RewardListResponse getRewardDetails(Long userId, RewardStatus status) {
        List<RewardRecord> records = status == null
                ? rewardRecordRepository.findByBeneficiaryUserIdOrderByIdDesc(userId)
                : rewardRecordRepository.findByBeneficiaryUserIdAndRewardStatusOrderByIdDesc(userId, status);
        List<RewardListItem> items = new ArrayList<>();
        for (RewardRecord record : records) {
            items.add(new RewardListItem(
                    record.getBeneficiaryUserId(),
                    record.getSourceUserId(),
                    record.getRewardLevel(),
                    record.getRewardAmount(),
                    record.getRewardStatus(),
                    record.getCalculatedAt()
            ));
        }
        return new RewardListResponse(items, items.size(), 0, items.size());
    }

    private long resolveEffectiveUsers(List<DistributionRelation> directRelations) {
        Map<Long, UserDistributionProfile> profileMap = loadProfileMap(directRelations);
        return directRelations.stream()
                .map(DistributionRelation::getUserId)
                .map(profileMap::get)
                .filter(UserDistributionProfile::isEffectiveUser)
                .count();
    }

    private Map<Long, UserDistributionProfile> loadProfileMap(List<DistributionRelation> relations) {
        List<Long> userIds = relations.stream().map(DistributionRelation::getUserId).toList();
        Map<Long, UserDistributionProfile> profileMap = new HashMap<>();
        for (UserDistributionProfile profile : userDistributionProfileRepository.findByUserIdIn(userIds)) {
            profileMap.put(profile.getUserId(), profile);
        }
        return profileMap;
    }
}
