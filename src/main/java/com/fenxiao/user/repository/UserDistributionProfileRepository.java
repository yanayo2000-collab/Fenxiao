package com.fenxiao.user.repository;

import com.fenxiao.user.entity.UserDistributionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserDistributionProfileRepository extends JpaRepository<UserDistributionProfile, Long> {
    Optional<UserDistributionProfile> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
    long countByEffectiveUserTrue();
    long countByUserIdIn(Collection<Long> userIds);
    long countByUserIdInAndEffectiveUserTrue(Collection<Long> userIds);
    List<UserDistributionProfile> findByUserIdIn(Collection<Long> userIds);
}
