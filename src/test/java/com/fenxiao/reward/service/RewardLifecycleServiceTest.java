package com.fenxiao.reward.service;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class RewardLifecycleServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Test
    void shouldUnlockFrozenRewardsWhenDue() {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(23001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(23002L, "ID", "id", inviterCode);

        rewardCalculationService.processIncomeEvent("evt-unfreeze-1", 23002L, new BigDecimal("100.00"), "USD", LocalDateTime.now().minusDays(8));

        int unlockedCount = rewardCalculationService.unlockDueRewards(LocalDateTime.now());

        List<RewardRecord> records = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-unfreeze-1");
        assertThat(unlockedCount).isEqualTo(1);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getRewardStatus()).isEqualTo(RewardStatus.AVAILABLE);
    }

    private void seedRules() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0);
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L, effectiveFrom, null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L, effectiveFrom, null));
    }
}
