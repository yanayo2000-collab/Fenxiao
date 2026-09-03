package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RewardEngineReportResponse;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.reward.service.RewardCalculationService;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class RewardEngineReportServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Autowired
    private RewardEngineReportService rewardEngineReportService;

    @Test
    void shouldReportBlockedLegacyCandidatesAndZeroNewMultilevelRewards() {
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.10"), 7, 1L));
        String rootCode = distributionBindingService.createProfile(19501L, "ID", "id", null).getInviteCode();
        String middleCode = distributionBindingService.createProfile(19502L, "ID", "id", rootCode).getInviteCode();
        distributionBindingService.createProfile(19503L, "ID", "id", middleCode);

        rewardCalculationService.processIncomeEvent(
                "evt-engine-report-1", 19503L, new BigDecimal("50.00"), "USD", LocalDateTime.now());

        RewardEngineReportResponse report = rewardEngineReportService.getReport();
        assertThat(report.engineEnabled()).isTrue();
        assertThat(report.engineVersion()).isEqualTo("BANDEIRA_V1_DIRECT_ONLY");
        assertThat(report.processedIncomeEvents()).isEqualTo(1);
        assertThat(report.generatedDirectRewards()).isEqualTo(1);
        assertThat(report.legacyRewardCandidates()).isEqualTo(2);
        assertThat(report.blockedLegacyRewardCandidates()).isEqualTo(1);
        assertThat(report.newMultilevelRewards()).isZero();
        assertThat(report.safetyGatePassed()).isTrue();
    }
}
