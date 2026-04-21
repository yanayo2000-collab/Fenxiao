package com.fenxiao.user.repository;

import com.fenxiao.user.entity.UserDistributionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDistributionProfileRepository extends JpaRepository<UserDistributionProfile, Long> {
    Optional<UserDistributionProfile> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
