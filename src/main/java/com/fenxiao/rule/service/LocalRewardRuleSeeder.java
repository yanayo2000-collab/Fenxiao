package com.fenxiao.rule.service;

import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
public class LocalRewardRuleSeeder implements CommandLineRunner {

    private static final String DEFAULT_COUNTRY_CODE = "ID";
    private static final String DEFAULT_ROLE_CODE = "NORMAL_USER";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int DEFAULT_FREEZE_DAYS = 7;
    private static final long SYSTEM_CREATED_BY = 0L;

    private final RewardRuleRepository rewardRuleRepository;
    private final Clock clock;

    @Autowired
    public LocalRewardRuleSeeder(RewardRuleRepository rewardRuleRepository) {
        this(rewardRuleRepository, Clock.systemUTC());
    }

    LocalRewardRuleSeeder(RewardRuleRepository rewardRuleRepository, Clock clock) {
        this.rewardRuleRepository = rewardRuleRepository;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        seedDefaults();
    }

    List<RewardRule> seedDefaults() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<RewardRule> seededRules = new ArrayList<>();
        seedIfMissing(seededRules, 1, new BigDecimal("0.15"), now);
        seedIfMissing(seededRules, 2, new BigDecimal("0.05"), now);
        seedIfMissing(seededRules, 3, new BigDecimal("0.02"), now);
        return seededRules;
    }

    private void seedIfMissing(List<RewardRule> seededRules, int rewardLevel, BigDecimal rewardRate, LocalDateTime now) {
        boolean exists = rewardRuleRepository.findEffectiveRule(
                DEFAULT_COUNTRY_CODE,
                DEFAULT_ROLE_CODE,
                rewardLevel,
                ACTIVE_STATUS,
                now
        ).isPresent();
        if (exists) {
            return;
        }
        RewardRule rule = RewardRule.create(
                DEFAULT_COUNTRY_CODE,
                DEFAULT_ROLE_CODE,
                rewardLevel,
                rewardRate,
                DEFAULT_FREEZE_DAYS,
                SYSTEM_CREATED_BY,
                now.minusMinutes(1),
                null
        );
        seededRules.add(rewardRuleRepository.save(rule));
    }
}
