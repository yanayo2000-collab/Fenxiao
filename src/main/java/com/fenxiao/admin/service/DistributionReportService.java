package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.OverviewReportResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DistributionReportService {

    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final RiskEventRepository riskEventRepository;
    private final AdminProductScopeService adminProductScopeService;

    public DistributionReportService(UserDistributionProfileRepository userDistributionProfileRepository,
                                     RewardRecordRepository rewardRecordRepository,
                                     RiskEventRepository riskEventRepository,
                                     AdminProductScopeService adminProductScopeService) {
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.riskEventRepository = riskEventRepository;
        this.adminProductScopeService = adminProductScopeService;
    }

    public OverviewReportResponse getOverview(String productCode) {
        String normalizedProductCode = adminProductScopeService.normalizeProductCode(productCode);
        if (normalizedProductCode == null) {
            return buildGlobalOverview();
        }

        List<Long> scopedUserIds = adminProductScopeService.resolveScopedUserIds(normalizedProductCode);
        if (scopedUserIds.isEmpty()) {
            return new OverviewReportResponse(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        long totalProfiles = userDistributionProfileRepository.countByUserIdIn(scopedUserIds);
        long effectiveUsers = userDistributionProfileRepository.countByUserIdInAndEffectiveUserTrue(scopedUserIds);
        long invitedUsers = Math.max(0, totalProfiles - 1);
        BigDecimal rewardTotal = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdIn(scopedUserIds);
        BigDecimal frozenRewardTotal = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdInAndStatus(scopedUserIds, RewardStatus.FROZEN)
                .add(rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdInAndStatus(scopedUserIds, RewardStatus.RISK_HOLD));
        BigDecimal availableRewardTotal = rewardRecordRepository.sumRewardAmountByBeneficiaryUserIdInAndStatus(scopedUserIds, RewardStatus.AVAILABLE);

        return new OverviewReportResponse(
                invitedUsers,
                effectiveUsers,
                rewardTotal,
                frozenRewardTotal,
                availableRewardTotal,
                (int) riskEventRepository.countByUserIdIn(scopedUserIds)
        );
    }

    private OverviewReportResponse buildGlobalOverview() {
        long totalProfiles = userDistributionProfileRepository.count();
        long effectiveUsers = userDistributionProfileRepository.countByEffectiveUserTrue();
        long invitedUsers = Math.max(0, totalProfiles - 1);
        BigDecimal rewardTotal = rewardRecordRepository.sumRewardAmount();
        BigDecimal frozenRewardTotal = rewardRecordRepository.sumRewardAmountByStatus(RewardStatus.FROZEN)
                .add(rewardRecordRepository.sumRewardAmountByStatus(RewardStatus.RISK_HOLD));
        BigDecimal availableRewardTotal = rewardRecordRepository.sumRewardAmountByStatus(RewardStatus.AVAILABLE);

        return new OverviewReportResponse(
                invitedUsers,
                effectiveUsers,
                rewardTotal,
                frozenRewardTotal,
                availableRewardTotal,
                riskEventRepository.count()
        );
    }
}
