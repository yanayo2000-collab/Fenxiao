package com.fenxiao.experiment;

import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.experiment.dto.*;
import com.fenxiao.experiment.service.ControlledExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class ControlledExperimentServiceTest {
    @Autowired ControlledExperimentService service;
    @Autowired DistributionBindingService bindingService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldKeepFixedDenominatorAndIdempotentMetrics() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC()).withNano(0);
        var user = bindingService.createProfile(73101L, "BR", "pt-br", null);
        service.create(new CreateExperimentRequest("V1_100_BR", "V1 100 users", 100, "FIRST_INCOME", now.minusHours(1), now.plusDays(7), now.plusDays(37)), 1L);
        service.changeStatus("V1_100_BR", new ExperimentStatusRequest("ENROLLING", "approved"), 1L);
        var enrolled = service.enroll("V1_100_BR", new EnrollParticipantRequest(user.getUserId(), "BR_LINKY", "{\"phoneVerified\":true}"), 1L);
        assertThat(enrolled.get("denominator")).isEqualTo(1);
        service.changeParticipantStatus("V1_100_BR", user.getUserId(), new ExperimentStatusRequest("ACTIVE", "started"), 1L);
        var event = new ExperimentMetricEventRequest(user.getUserId(), "FIRST_INCOME", BigDecimal.ONE, "NIUMA", "income-73101", now.plusHours(1));
        assertThat(service.recordMetric("V1_100_BR", event)).isTrue();
        assertThat(service.recordMetric("V1_100_BR", event)).isFalse();
        service.changeParticipantStatus("V1_100_BR", user.getUserId(), new ExperimentStatusRequest("WITHDRAWN", "user opted out"), 1L);
        var dashboard = service.dashboard("V1_100_BR");
        assertThat(dashboard.get("fixedDenominator")).isEqualTo(1);
        assertThat(dashboard.get("withdrawn")).isEqualTo(1);
        assertThat(dashboard.get("convertedCount")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from experiment_metric_event", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldRejectSampleLargerThanOneHundred() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        assertThatThrownBy(() -> service.create(new CreateExperimentRequest("TOO_BIG", "Too big", 101, "FIRST_INCOME", now, now.plusDays(1), now.plusDays(2)), 1L))
                .isInstanceOf(Exception.class);
    }
}
