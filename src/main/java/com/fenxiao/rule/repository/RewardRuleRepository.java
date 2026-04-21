package com.fenxiao.rule.repository;

import com.fenxiao.rule.entity.RewardRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RewardRuleRepository extends JpaRepository<RewardRule, Long> {

    @Query("""
            select r from RewardRule r
            where r.countryCode = :countryCode
              and r.roleCode = :roleCode
              and r.rewardLevel = :rewardLevel
              and r.status = :status
              and r.effectiveFrom <= :eventTime
              and (r.effectiveTo is null or r.effectiveTo >= :eventTime)
            order by r.effectiveFrom desc
            """)
    Optional<RewardRule> findEffectiveRule(@Param("countryCode") String countryCode,
                                           @Param("roleCode") String roleCode,
                                           @Param("rewardLevel") Integer rewardLevel,
                                           @Param("status") String status,
                                           @Param("eventTime") LocalDateTime eventTime);
}
