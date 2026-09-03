package com.fenxiao.incentive;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.incentive.dto.*;
import com.fenxiao.incentive.service.IncentiveShadowService;
import com.fenxiao.platform.domain.PlatformFactType;
import com.fenxiao.platform.dto.PlatformBusinessFactRequest;
import com.fenxiao.platform.dto.VerifyPlatformBindingRequest;
import com.fenxiao.platform.service.PlatformLifecycleService;
import com.fenxiao.relationship.service.RelationshipFoundationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class IncentiveShadowServiceTest {
    @Autowired DistributionBindingService bindingService;
    @Autowired RelationshipFoundationService relationshipService;
    @Autowired PlatformLifecycleService lifecycleService;
    @Autowired IncentiveShadowService incentiveService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldKeepMentorAndFivePercentTeamRewardsInSeparateShadowLedgers() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).withNano(0);
        var leader = bindingService.createProfile(72100L, "BR", "pt-br", null);
        var mentor = bindingService.createProfile(72101L, "BR", "pt-br", leader.getInviteCode());
        relationshipService.qualifyMentor(mentor.getUserId(), "BR", "pt-br", 20);
        var source = bindingService.createProfile(72102L, "BR", "pt-br", leader.getInviteCode());

        incentiveService.configureMentorRule(new MentorRewardRuleRequest("MENTOR_72H_BR", "VALID_72H_START", "LINKY", "BR", null, 200, "BRL", 7, now.minusMinutes(1)));
        incentiveService.configureLeadershipPolicy(new LeadershipPolicyRequest("NEW_STAR_BR_TEST", "LINKY", "BR", "BR_GUILD_1", 1, 1, 0, new BigDecimal("0.05"), now.minusMinutes(1)));
        lifecycleService.configurePolicy("LINKY", "BR_GUILD_1", "BR", new BigDecimal("10.00"), "DIAMOND", now.minusMinutes(1));
        lifecycleService.submit(leader.getUserId(), "LINKY", "22345670");
        lifecycleService.verify(new VerifyPlatformBindingRequest("LINKY", "22345670", false, true, "BR_GUILD_1", now, "NIUMA_PLATFORM_FACTS", "verify-72100"));
        lifecycleService.submit(source.getUserId(), "LINKY", "22345678");
        lifecycleService.verify(new VerifyPlatformBindingRequest("LINKY", "22345678", false, true, "BR_GUILD_1", now, "NIUMA_PLATFORM_FACTS", "verify-72102"));
        lifecycleService.ingest(new PlatformBusinessFactRequest("income-72102", "LINKY", "22345678", PlatformFactType.NET_INCOME,
                new BigDecimal("10.00"), "DIAMOND", now.plusHours(1), "BR_GUILD_1", "NIUMA_PLATFORM_FACTS", "v1", "hash-72102"));

        assertThat(jdbc.queryForObject("select count(*) from incentive_shadow_ledger where recipient_user_id=? and source_user_id=? and reward_type='MENTOR' and ledger_status='SHADOW'", Integer.class, mentor.getUserId(), source.getUserId())).isEqualTo(1);
        assertThat(incentiveService.evaluateLeadership(leader.getUserId(), "LINKY").profitShareQualified()).isTrue();

        var team = relationshipService.createTeam("BR-TEST-721", "BR Test Team", "BR", leader.getUserId());
        var result = incentiveService.ingestTeamProfit(new TeamProfitFactRequest("profit-721", team.getId(), "LINKY", LocalDate.now().minusDays(7), LocalDate.now(),
                100_000, 20_000, 10_000, 5_000, 5_000, "BRL", "NIUMA_PLATFORM_FACTS"));
        assertThat(result.operatingProfitMinor()).isEqualTo(60_000);
        assertThat(result.shareAmountMinor()).isEqualTo(3_000);
        assertThat(result.shadowLedgerCreated()).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from team_profit_share_shadow_ledger where ledger_status='SHADOW'", Integer.class)).isEqualTo(1);
    }
}
