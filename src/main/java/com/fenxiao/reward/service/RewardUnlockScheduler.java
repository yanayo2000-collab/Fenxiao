package com.fenxiao.reward.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class RewardUnlockScheduler {

    private final RewardCalculationService rewardCalculationService;

    public RewardUnlockScheduler(RewardCalculationService rewardCalculationService) {
        this.rewardCalculationService = rewardCalculationService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void unlockDueRewards() {
        rewardCalculationService.unlockDueRewards(LocalDateTime.now(Clock.systemUTC()));
    }
}
