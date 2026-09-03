package com.fenxiao.reward.config;

import com.fenxiao.common.api.ServiceUnavailableException;
import com.fenxiao.reward.domain.RewardEngineVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.distribution.reward-engine")
public class RewardEngineProperties {

    private boolean enabled = true;
    private RewardEngineVersion version = RewardEngineVersion.BANDEIRA_V1_DIRECT_ONLY;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RewardEngineVersion getVersion() {
        return version;
    }

    public void setVersion(RewardEngineVersion version) {
        this.version = version;
    }

    public void assertProcessingEnabled() {
        if (!enabled) {
            throw new ServiceUnavailableException("reward engine is disabled");
        }
        if (version != RewardEngineVersion.BANDEIRA_V1_DIRECT_ONLY) {
            throw new ServiceUnavailableException("unsupported reward engine version");
        }
    }
}
