package com.fenxiao.reward.service;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.distribution.domain.LockStatus;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.income.entity.IncomeEvent;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.domain.RewardType;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private IncomeEventRepository incomeEventRepository;

    @Autowired
    private UserDistributionProfileRepository userDistributionProfileRepository;

    @Autowired
    private DistributionRelationRepository distributionRelationRepository;

    @Test
    void shouldGenerateOnlyDirectRewardAndRemainIdempotent() {
        seedRules();
        distributionBindingService.createProfile(9001L, "ID", "id", null);
        String rootInviteCode = distributionBindingService.createProfile(9002L, "ID", "id", null).getInviteCode();
        String level1InviteCode = distributionBindingService.createProfile(9003L, "ID", "id", rootInviteCode).getInviteCode();
        String level2InviteCode = distributionBindingService.createProfile(9004L, "ID", "id", level1InviteCode).getInviteCode();
        distributionBindingService.createProfile(9005L, "ID", "id", level2InviteCode);

        rewardCalculationService.processIncomeEvent("evt-100", 9005L, new BigDecimal("100.00"), "USD", LocalDateTime.now());
        rewardCalculationService.processIncomeEvent("evt-100", 9005L, new BigDecimal("100.00"), "USD", LocalDateTime.now());

        List<RewardRecord> records = rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-100");
        assertThat(records).hasSize(1);
        assertThat(records).extracting(RewardRecord::getBeneficiaryUserId)
                .containsExactly(9004L);
        assertThat(records).extracting(RewardRecord::getRewardAmount)
                .containsExactly(new BigDecimal("10.000000"));
        assertThat(records).extracting(RewardRecord::getRewardStatus)
                .containsOnly(RewardStatus.FROZEN);
        assertThat(records).extracting(RewardRecord::getRewardType)
                .containsOnly(RewardType.DIRECT_RECRUIT);
        assertThat(records).extracting(RewardRecord::getRewardEngineVersion)
                .containsOnly("BANDEIRA_V1_DIRECT_ONLY");

        IncomeEvent event = incomeEventRepository.findBySourceEventId("evt-100").orElseThrow();
        assertThat(event.getRewardEngineVersion()).isEqualTo("BANDEIRA_V1_DIRECT_ONLY");
        assertThat(event.getLegacyCandidateCount()).isEqualTo(3);
        assertThat(event.getGeneratedRewardCount()).isEqualTo(1);
        assertThat(event.getRewardProcessingStatus()).isEqualTo("COMPLETED");
        assertThat(event.getRewardDecisionJson()).contains("\"blockedLevels\":[2,3]");
        assertThat(incomeEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldFailBeforeAnyWriteWhenDirectRuleIsMissing() {
        String inviterCode = distributionBindingService.createProfile(9051L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(9052L, "ID", "id", inviterCode);

        assertThatThrownBy(() -> rewardCalculationService.processIncomeEvent(
                "evt-no-rule", 9052L, new BigDecimal("100.00"), "USD", LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("exactly one direct reward rule is required");

        assertThat(incomeEventRepository.findBySourceEventId("evt-no-rule")).isEmpty();
        assertThat(rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-no-rule")).isEmpty();
        assertThat(userDistributionProfileRepository.findById(9052L).orElseThrow().getConfirmedIncomeTotal())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(distributionRelationRepository.findByUserId(9052L).orElseThrow().getLockStatus())
                .isEqualTo(LockStatus.UNLOCKED);
    }

    @Test
    void shouldFailClosedWhenDirectRulesOverlap() {
        LocalDateTime now = LocalDateTime.now();
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.10"), 7, 1L,
                now.minusDays(2), null));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.20"), 7, 1L,
                now.minusDays(1), null));
        String inviterCode = distributionBindingService.createProfile(9061L, "ID", "id", null).getInviteCode();
        distributionBindingService.createProfile(9062L, "ID", "id", inviterCode);

        assertThatThrownBy(() -> rewardCalculationService.processIncomeEvent(
                "evt-overlap", 9062L, new BigDecimal("100.00"), "USD", now))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("exactly one direct reward rule is required");
        assertThat(incomeEventRepository.findBySourceEventId("evt-overlap")).isEmpty();
    }

    @Test
    void shouldKeepLegacyRewardReadOnlyAndOutOfUnlockFlow() {
        RewardRecord legacyReward = RewardRecord.create(
                "evt-legacy-read-only",
                9071L,
                9072L,
                2,
                new BigDecimal("100.000000"),
                new BigDecimal("0.020000"),
                new BigDecimal("2.000000"),
                "USD",
                0,
                LocalDateTime.now().minusDays(1),
                "LEGACY_V0",
                RewardType.LEGACY_LEVEL
        );
        rewardRecordRepository.save(legacyReward);

        assertThat(rewardCalculationService.unlockDueRewards(LocalDateTime.now())).isZero();
        assertThat(rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-legacy-read-only").get(0).getRewardStatus())
                .isEqualTo(RewardStatus.FROZEN);
        assertThatThrownBy(legacyReward::markAvailable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("legacy or multilevel reward is read-only");
    }

    @Test
    void shouldKeepHistoricalDirectRewardInLifecycle() {
        RewardRecord historicalDirectReward = RewardRecord.create(
                "evt-legacy-direct",
                9081L,
                9082L,
                1,
                new BigDecimal("100.000000"),
                new BigDecimal("0.100000"),
                new BigDecimal("10.000000"),
                "USD",
                0,
                LocalDateTime.now().minusDays(1),
                "LEGACY_V0",
                RewardType.DIRECT_RECRUIT
        );
        rewardRecordRepository.save(historicalDirectReward);

        assertThat(rewardCalculationService.unlockDueRewards(LocalDateTime.now())).isEqualTo(1);
        assertThat(rewardRecordRepository.findById(historicalDirectReward.getId()).orElseThrow().getRewardStatus())
                .isEqualTo(RewardStatus.AVAILABLE);
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
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.10"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.02"), 7, 1L));
        rewardRuleRepository.save(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.005"), 7, 1L));
    }
}
