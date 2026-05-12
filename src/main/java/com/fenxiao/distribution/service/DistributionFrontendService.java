package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.DistributionHomeResponse;
import com.fenxiao.distribution.api.dto.TeamListResponse;
import com.fenxiao.distribution.api.dto.TeamMemberItem;
import com.fenxiao.distribution.api.dto.WeeklyIncomeStatsResponse;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.api.dto.RewardListItem;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.api.dto.RewardSummaryResponse;
import com.fenxiao.reward.api.dto.RewardTierSummaryItem;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
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
    private final IncomeEventRepository incomeEventRepository;
    private final Clock clock;

    public DistributionFrontendService(UserDistributionProfileRepository userDistributionProfileRepository,
                                       DistributionRelationRepository distributionRelationRepository,
                                       RewardRecordRepository rewardRecordRepository,
                                       IncomeEventRepository incomeEventRepository) {
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.distributionRelationRepository = distributionRelationRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.clock = Clock.systemUTC();
    }

    public DistributionHomeResponse getHome(Long userId) {
        UserDistributionProfile profile = userDistributionProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution profile not found"));
        DistributionRelation relation = distributionRelationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));
        List<DistributionRelation> directRelations = distributionRelationRepository.findByLevel1InviterIdOrderByIdDesc(userId);
        List<DistributionRelation> secondLevelRelations = distributionRelationRepository.findByLevel2InviterIdOrderByIdDesc(userId);
        List<DistributionRelation> thirdLevelRelations = distributionRelationRepository.findByLevel3InviterIdOrderByIdDesc(userId);

        long directEffectiveUsers = resolveEffectiveUsers(directRelations);
        long secondLevelEffectiveUsers = resolveEffectiveUsers(secondLevelRelations);
        long thirdLevelEffectiveUsers = resolveEffectiveUsers(thirdLevelRelations);
        long totalTeamUsers = directRelations.size() + secondLevelRelations.size() + thirdLevelRelations.size();
        long totalEffectiveUsers = directEffectiveUsers + secondLevelEffectiveUsers + thirdLevelEffectiveUsers;
        BigDecimal totalReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserId(userId);
        BigDecimal frozenReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndStatus(userId, RewardStatus.FROZEN);
        BigDecimal availableReward = rewardRecordRepository.sumWithdrawableRewardAmountByBeneficiaryUserId(userId);
        BigDecimal riskHoldReward = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndStatus(userId, RewardStatus.RISK_HOLD);

        return new DistributionHomeResponse(
                userId,
                profile.getInviteCode(),
                relation.getLevel1InviterId(),
                directRelations.size(),
                totalEffectiveUsers,
                totalReward,
                frozenReward,
                availableReward,
                riskHoldReward,
                directRelations.size(),
                secondLevelRelations.size(),
                thirdLevelRelations.size(),
                totalTeamUsers,
                directEffectiveUsers,
                secondLevelEffectiveUsers,
                thirdLevelEffectiveUsers,
                totalEffectiveUsers
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

    public RewardSummaryResponse getRewardSummary(Long userId) {
        List<RewardTierSummaryItem> tiers = new ArrayList<>();
        for (int level = 1; level <= 3; level++) {
            tiers.add(new RewardTierSummaryItem(
                    level,
                    switch (level) {
                        case 1 -> "业务二级收益";
                        case 2 -> "业务三级收益";
                        default -> "业务四级收益";
                    },
                    rewardRecordRepository.countByBeneficiaryUserIdAndRewardLevel(userId, level),
                    rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndRewardLevel(userId, level)
            ));
        }
        return new RewardSummaryResponse(userId, tiers);
    }

    public WeeklyIncomeStatsResponse getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now(clock);
        WeekFields weekFields = WeekFields.ISO;
        LocalDate currentStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate previousStart = currentStart.minusWeeks(1);
        LocalDateTime currentStartAt = currentStart.atStartOfDay();
        LocalDateTime currentEndAt = currentStart.plusWeeks(1).atStartOfDay();
        LocalDateTime previousStartAt = previousStart.atStartOfDay();
        LocalDateTime previousEndAt = currentStart.atStartOfDay();
        return new WeeklyIncomeStatsResponse(
                userId,
                weekLabel(currentStart),
                weekLabel(previousStart),
                incomeEventRepository.sumIncomeAmountByUserIdAndEventTimeBetween(userId, currentStartAt, currentEndAt),
                incomeEventRepository.sumIncomeAmountByUserIdAndEventTimeBetween(userId, previousStartAt, previousEndAt),
                rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndCalculatedAtBetween(userId, currentStartAt, currentEndAt),
                rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdAndCalculatedAtBetween(userId, previousStartAt, previousEndAt)
        );
    }

    private String weekLabel(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        return "%d-W%02d".formatted(date.get(weekFields.weekBasedYear()), date.get(weekFields.weekOfWeekBasedYear()));
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
