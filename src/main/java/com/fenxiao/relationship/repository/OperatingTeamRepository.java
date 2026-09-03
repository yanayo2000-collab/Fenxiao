package com.fenxiao.relationship.repository;

import com.fenxiao.relationship.entity.OperatingTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OperatingTeamRepository extends JpaRepository<OperatingTeam,Long> {
    Optional<OperatingTeam> findByTeamCode(String teamCode);
}
