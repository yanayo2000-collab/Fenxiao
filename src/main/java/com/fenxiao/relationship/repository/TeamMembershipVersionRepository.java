package com.fenxiao.relationship.repository;

import com.fenxiao.relationship.entity.TeamMembershipVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamMembershipVersionRepository extends JpaRepository<TeamMembershipVersion,Long> {
    Optional<TeamMembershipVersion> findTopByUserIdOrderByVersionNoDesc(Long userId);
}
