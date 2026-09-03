package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RewardEngineReportResponse;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.config.RewardEngineProperties;
import com.fenxiao.reward.repository.RewardRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RewardEngineReportService {

    private static final String LEGACY_ENGINE_VERSION = "LEGACY_V0";

    private final IncomeEventRepository incomeEventRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final RewardEngineProperties rewardEngineProperties;

    public RewardEngineReportService(IncomeEventRepository incomeEventRepository,
                                     RewardRecordRepository rewardRecordRepository,
                                     RewardEngineProperties rewardEngineProperties) {
        this.incomeEventRepository = incomeEventRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.rewardEngineProperties = rewardEngineProperties;
    }

    public RewardEngineReportResponse getReport() {
        String engineVersion = rewardEngineProperties.getVersion().name();
        long processedIncomeEvents = incomeEventRepository.countByRewardEngineVersion(engineVersion);
        long generatedDirectRewards = incomeEventRepository.sumGeneratedRewardCountByRewardEngineVersion(engineVersion);
        long legacyRewardCandidates = incomeEventRepository.sumLegacyCandidateCountByRewardEngineVersion(engineVersion);
        long blockedLegacyRewardCandidates = Math.max(0, legacyRewardCandidates - generatedDirectRewards);
        long newMultilevelRewards = rewardRecordRepository.countByRewardEngineVersionAndRewardLevelGreaterThan(engineVersion, 1);
        long historicalLegacyRewards = rewardRecordRepository.countByRewardEngineVersion(LEGACY_ENGINE_VERSION);

        return new RewardEngineReportResponse(
                rewardEngineProperties.isEnabled(),
                engineVersion,
                processedIncomeEvents,
                generatedDirectRewards,
                legacyRewardCandidates,
                blockedLegacyRewardCandidates,
                newMultilevelRewards,
                historicalLegacyRewards,
                newMultilevelRewards == 0
        );
    }
}
