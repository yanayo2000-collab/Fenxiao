package com.fenxiao.platform.repository;

import com.fenxiao.platform.entity.PlatformMilestonePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface PlatformMilestonePolicyRepository extends JpaRepository<PlatformMilestonePolicy, Long> {
    Optional<PlatformMilestonePolicy> findTopByPlatformCodeAndGuildIdAndCountryCodeAndEnabledTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
            String platformCode, String guildId, String countryCode, LocalDateTime at);
    List<PlatformMilestonePolicy> findByPlatformCodeAndGuildIdAndCountryCodeAndEnabledTrue(String platformCode, String guildId, String countryCode);
}
