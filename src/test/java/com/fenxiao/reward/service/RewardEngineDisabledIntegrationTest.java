package com.fenxiao.reward.service;

import com.fenxiao.common.api.ServiceUnavailableException;
import com.fenxiao.income.entity.IncomeEvent;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Transactional
@SpringBootTest(properties = "app.distribution.reward-engine.enabled=false")
class RewardEngineDisabledIntegrationTest {

    @Autowired
    private RewardCalculationService rewardCalculationService;

    @Autowired
    private IncomeEventRepository incomeEventRepository;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Test
    void shouldRejectNewEventsWithoutWritesButStillAcknowledgeDuplicates() {
        LocalDateTime now = LocalDateTime.now();
        incomeEventRepository.save(IncomeEvent.create(
                "evt-disabled-existing",
                9991L,
                "BR",
                new BigDecimal("1.00"),
                "BRL",
                now,
                "BANDEIRA_V1_DIRECT_ONLY",
                0,
                "{\"mode\":\"DIRECT_ONLY\"}"
        ));

        assertThat(rewardCalculationService.processIncomeEvent(
                "evt-disabled-existing", 9991L, new BigDecimal("1.00"), "BRL", now))
                .isEqualTo(IncomeProcessStatus.DUPLICATE);

        assertThatThrownBy(() -> rewardCalculationService.processIncomeEvent(
                "evt-disabled-new", 9992L, new BigDecimal("1.00"), "BRL", now))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("reward engine is disabled");

        assertThat(incomeEventRepository.findBySourceEventId("evt-disabled-new")).isEmpty();
        assertThat(rewardRecordRepository.findBySourceEventIdOrderByRewardLevelAsc("evt-disabled-new")).isEmpty();
    }

    @Test
    void shouldRejectUnlockWithoutChangingRewards() {
        LocalDateTime now = LocalDateTime.now();
        RewardRecord reward = rewardRecordRepository.save(RewardRecord.create(
                "evt-disabled-unlock",
                9993L,
                9994L,
                1,
                new BigDecimal("100.00"),
                new BigDecimal("0.10"),
                new BigDecimal("10.00"),
                "BRL",
                7,
                now.minusDays(10)
        ));

        assertThatThrownBy(() -> rewardCalculationService.unlockDueRewards(now))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("reward engine is disabled");

        assertThat(rewardRecordRepository.findById(reward.getId()).orElseThrow().getRewardStatus())
                .isEqualTo(RewardStatus.FROZEN);
    }
}
