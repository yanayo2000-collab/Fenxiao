package com.fenxiao.reward.config;

import com.fenxiao.reward.domain.RewardEngineVersion;
import com.fenxiao.common.api.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewardEnginePropertiesTest {

    @Test
    void shouldFailClosedWhenEngineIsDisabled() {
        RewardEngineProperties properties = new RewardEngineProperties();
        properties.setEnabled(false);

        assertThatThrownBy(properties::assertProcessingEnabled)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("reward engine is disabled");
    }

    @Test
    void shouldAllowOnlyBandeiraDirectEngine() {
        RewardEngineProperties properties = new RewardEngineProperties();
        properties.setVersion(RewardEngineVersion.BANDEIRA_V1_DIRECT_ONLY);

        assertThatCode(properties::assertProcessingEnabled).doesNotThrowAnyException();
    }
}
