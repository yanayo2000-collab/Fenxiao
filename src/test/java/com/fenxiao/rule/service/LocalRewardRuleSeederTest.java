package com.fenxiao.rule.service;

import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalRewardRuleSeederTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-24T09:10:00Z"), ZoneOffset.UTC);

    @Test
    void shouldSeedDefaultRulesWhenLocalRulesAreMissing() {
        RewardRuleRepository repository = mock(RewardRuleRepository.class);
        when(repository.findEffectiveRule(eq("ID"), eq("NORMAL_USER"), any(Integer.class), eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        LocalRewardRuleSeeder seeder = new LocalRewardRuleSeeder(repository, FIXED_CLOCK);

        seeder.seedDefaults();

        verify(repository, times(3)).save(any(RewardRule.class));
    }

    @Test
    void shouldSkipSavingRulesThatAlreadyExist() {
        RewardRuleRepository repository = mock(RewardRuleRepository.class);
        when(repository.findEffectiveRule(eq("ID"), eq("NORMAL_USER"), eq(1), eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(RewardRule.create("ID", "NORMAL_USER", 1, new BigDecimal("0.15"), 7, 1L)));
        when(repository.findEffectiveRule(eq("ID"), eq("NORMAL_USER"), eq(2), eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(RewardRule.create("ID", "NORMAL_USER", 2, new BigDecimal("0.05"), 7, 1L)));
        when(repository.findEffectiveRule(eq("ID"), eq("NORMAL_USER"), eq(3), eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(RewardRule.create("ID", "NORMAL_USER", 3, new BigDecimal("0.02"), 7, 1L)));

        LocalRewardRuleSeeder seeder = new LocalRewardRuleSeeder(repository, FIXED_CLOCK);

        seeder.seedDefaults();

        verify(repository, never()).save(any(RewardRule.class));
    }

    @Test
    void shouldSeedExpectedDefaultRates() {
        RewardRuleRepository repository = mock(RewardRuleRepository.class);
        when(repository.findEffectiveRule(eq("ID"), eq("NORMAL_USER"), any(Integer.class), eq("ACTIVE"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(repository.save(any(RewardRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalRewardRuleSeeder seeder = new LocalRewardRuleSeeder(repository, FIXED_CLOCK);

        List<RewardRule> seededRules = seeder.seedDefaults();

        assertThat(seededRules)
                .extracting(RewardRule::getRewardLevel, RewardRule::getRewardRate, RewardRule::getFreezeDays)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1, new BigDecimal("0.15"), 7),
                        org.assertj.core.groups.Tuple.tuple(2, new BigDecimal("0.05"), 7),
                        org.assertj.core.groups.Tuple.tuple(3, new BigDecimal("0.02"), 7)
                );
    }
}
