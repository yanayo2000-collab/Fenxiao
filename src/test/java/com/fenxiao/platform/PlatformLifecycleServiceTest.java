package com.fenxiao.platform;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.platform.domain.PlatformBindingStatus;
import com.fenxiao.platform.domain.PlatformFactType;
import com.fenxiao.platform.dto.PlatformBusinessFactRequest;
import com.fenxiao.platform.dto.VerifyPlatformBindingRequest;
import com.fenxiao.platform.repository.PlatformBusinessFactRepository;
import com.fenxiao.platform.service.PlatformLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class PlatformLifecycleServiceTest {
    @Autowired DistributionBindingService bindingService;
    @Autowired PlatformLifecycleService lifecycleService;
    @Autowired PlatformBusinessFactRepository factRepository;

    @Test
    void shouldVerifyByAuthoritativeJoinDateAndEvaluateSevenConsecutiveIncomeDaysIdempotently() {
        var root = bindingService.createProfile(71100L, "BR", "pt-br", null);
        var user = bindingService.createProfile(71101L, "BR", "pt-br", root.getInviteCode());
        LocalDateTime submitted = LocalDateTime.now(Clock.systemUTC()).withNano(0);
        lifecycleService.submit(user.getUserId(), "LINKY", "12345678");
        var verified = lifecycleService.verify(new VerifyPlatformBindingRequest(
                "LINKY", "12345678", false, true, "BR_GUILD_1", submitted,
                "NIUMA_PLATFORM_FACTS", "verification-1"));
        assertThat(verified.getBindingStatus()).isEqualTo(PlatformBindingStatus.VERIFIED);
        lifecycleService.configurePolicy("LINKY", "BR_GUILD_1", "BR", new BigDecimal("50.00"), "DIAMOND", submitted.minusMinutes(1));

        for (int day = 0; day < 7; day++) {
            lifecycleService.ingest(new PlatformBusinessFactRequest(
                    "income-" + day, "LINKY", "12345678", PlatformFactType.NET_INCOME,
                    new BigDecimal("10.00"), "DIAMOND", submitted.plusDays(day).plusHours(2),
                    "BR_GUILD_1", "NIUMA_PLATFORM_FACTS", "v1", "hash-" + day));
        }
        var snapshot = lifecycleService.get(user.getUserId(), "LINKY");
        assertThat(snapshot.isBindingVerified()).isTrue();
        assertThat(snapshot.isConsecutive7DayActive()).isTrue();
        assertThat(snapshot.isConsecutive30DayActive()).isFalse();
        assertThat(snapshot.getConsecutiveActiveDays()).isEqualTo(7);
        assertThat(snapshot.getCumulativeNetIncome()).isEqualByComparingTo("70.00");
        assertThat(snapshot.getFirstWithdrawEligibleAt()).isEqualTo(submitted.plusDays(4).plusHours(2));
        assertThat(snapshot.isShadowOnly()).isTrue();

        lifecycleService.ingest(new PlatformBusinessFactRequest(
                "income-0", "LINKY", "12345678", PlatformFactType.NET_INCOME,
                new BigDecimal("10.00"), "DIAMOND", submitted.plusHours(2),
                "BR_GUILD_1", "NIUMA_PLATFORM_FACTS", "v1", "hash-0"));
        assertThat(factRepository.count()).isEqualTo(7);
    }

    @Test
    void shouldRejectPreexistingGlobalIdInsteadOfClaimingIt() {
        var root = bindingService.createProfile(71200L, "BR", "pt-br", null);
        var user = bindingService.createProfile(71201L, "BR", "pt-br", root.getInviteCode());
        lifecycleService.submit(user.getUserId(), "TIMO", "123456789012");
        var rejected = lifecycleService.verify(new VerifyPlatformBindingRequest(
                "TIMO", "123456789012", true, true, "TIMO_BR", LocalDateTime.now(),
                "MCN_TOOL", "verification-2"));
        assertThat(rejected.getBindingStatus()).isEqualTo(PlatformBindingStatus.REJECTED);
        assertThat(rejected.getRejectionCode()).isEqualTo("PREEXISTING_GLOBAL_ID");
    }
}
