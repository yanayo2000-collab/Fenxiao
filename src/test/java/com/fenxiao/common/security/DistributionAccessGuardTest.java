package com.fenxiao.common.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DistributionAccessGuardTest {

    @Test
    void shouldRejectLinkyRequestWhenSigningSecretIsMissing() {
        DistributionAccessGuard guard = new DistributionAccessGuard(
                "internal-token",
                "admin-token",
                "profile-create-token",
                "",
                900,
                false,
                null,
                null,
                null,
                Clock.fixed(Instant.parse("2026-08-17T00:00:00Z"), ZoneOffset.UTC)
        );

        DistributionAccessGuard.LinkyRequestCheckResult result = guard.inspectLinkySignature(
                "2026-08-17T00:00:00Z",
                "signature",
                "linky-order-1",
                1L,
                "10.00",
                "USD",
                "2026-08-17T00:00:00Z"
        );

        assertThat(result.allowed()).isFalse();
        assertThat(result.signatureStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(result.replayStatus()).isEqualTo("NOT_CHECKED");
        assertThat(result.message()).isEqualTo("linky signature verification not configured");
    }
}
