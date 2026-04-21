package com.fenxiao.reward.service;

import com.fenxiao.distribution.domain.LockStatus;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
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
class RewardMvpFlowTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private RewardRuleRepository rewardRuleRepository;

    @Autowired
    private UserDistributionProfileRepository userDistributionProfileRepository;

    @Autowired
    private DistributionRelationRepository distributionRelationRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Test
    void shouldLockRelationAndMarkUserEffectiveOnFirstIncome() {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(13001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(13002L, "ID", "id", inviterCode);

        rewardCalculationService.processIncomeEvent("evt-lock-1", 13002L, new BigDecimal("20.00"), "USD", LocalDateTime.now());

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(13002L).orElseThrow();
        DistributionRelation relation = distributionRelationRepository.findByUserId(13002L).orElseThrow();

        assertThat(sourceUser.isEffectiveUser()).isTrue();
        assertThat(sourceUser.getConfirmedIncomeTotal()).isEqualByComparingTo("20.000000");
        assertThat(relation.getLockStatus()).isEqualTo(LockStatus.LOCKED);
    }

    @Test
    void shouldCreateRiskEventAndRiskHoldRewardsForRiskUser() {
        seedRules();
        String inviterCode = distributionBindingService.createProfile(14001L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(14002L, "ID", "id", inviterCode);

        UserDistributionProfile sourceUser = userDistributionProfileRepository.findById(14002L).orElseThrow();
        sourceUser.markAsRiskUser();
        userDistributionProfileRepository.save(sourceUser);

        rewardCalculationService.processIncomeEvent("evt-risk-hold-1", 14002L, new BigDecimal("40.00"), "USD", LocalDateTime.now());

        List<RewardRecord> records = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-risk-hold-1");
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getRewardStatus()).isEqualTo(RewardStatus.RISK_HOLD);
        assertThat(records.get(0).isRiskFlag()).isTrue();
        assertThat(riskEventRepository.findAll()).hasSize(1);
    }

    private void seedRules() {
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L));
    }
}
