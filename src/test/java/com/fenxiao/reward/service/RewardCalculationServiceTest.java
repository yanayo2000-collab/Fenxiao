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
class RewardCalculationServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Test
    void shouldGenerateThreeLevelRewardsAndRemainIdempotent() {
        seedRules();
        distributionBindingService.createProfile(9001L, "ID", "id", null);
        String rootInviteCode = distributionBindingService.createProfile(9002L, "ID", "id", null).getInviteCode();
        String level1InviteCode = distributionBindingService.createProfile(9003L, "ID", "id", rootInviteCode).getInviteCode();
        String level2InviteCode = distributionBindingService.createProfile(9004L, "ID", "id", level1InviteCode).getInviteCode();
        distributionBindingService.createProfile(9005L, "ID", "id", level2InviteCode);

        rewardCalculationService.processIncomeEvent("evt-100", 9005L, new BigDecimal("100.00"), "USD", LocalDateTime.now());
        rewardCalculationService.processIncomeEvent("evt-100", 9005L, new BigDecimal("100.00"), "USD", LocalDateTime.now());

        List<RewardRecord> records = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-100");
        assertThat(records).hasSize(3);
        assertThat(records).extracting(RewardRecord::getBeneficiaryUserId)
                .containsExactly(9004L, 9003L, 9002L);
        assertThat(records).extracting(RewardRecord::getRewardAmount)
                .containsExactly(new BigDecimal("15.000000"), new BigDecimal("5.000000"), new BigDecimal("2.000000"));
        assertThat(records).extracting(RewardRecord::getRewardStatus)
                .containsOnly(RewardStatus.FROZEN);
    }

    @Test
    void shouldPickRuleByEventTime() {
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.10"), 7, 1L,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59)));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.20"), 7, 1L,
                LocalDateTime.of(2026, 2, 1, 0, 0),
                null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L));

        String inviterCode = distributionBindingService.createProfile(9101L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(9102L, "ID", "id", inviterCode);

        rewardCalculationService.processIncomeEvent("evt-200", 9102L, new BigDecimal("100.00"), "USD", LocalDateTime.of(2026, 1, 15, 12, 0));

        RewardRecord record = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-200").get(0);
        assertThat(record.getRewardAmount()).isEqualTo(new BigDecimal("10.000000"));
    }

    private void seedRules() {
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L));
    }
}
