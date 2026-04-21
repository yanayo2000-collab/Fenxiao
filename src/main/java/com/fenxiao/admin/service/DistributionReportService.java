package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.OverviewReportResponse;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class DistributionReportService {

    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final RiskEventRepository riskEventRepository;

    public DistributionReportService(UserDistributionProfileRepository userDistributionProfileRepository,
                                     RewardRecordRepository rewardRecordRepository,
                                     RiskEventRepository riskEventRepository) {
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.riskEventRepository = riskEventRepository;
    }

    public OverviewReportResponse getOverview() {
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
