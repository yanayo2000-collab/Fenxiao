package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.DistributionRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DistributionRelationRepository extends JpaRepository<DistributionRelation, Long> {
    Optional<DistributionRelation> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<DistributionRelation> findByLevel1InviterIdOrderByIdDesc(Long level1InviterId);

    List<DistributionRelation> findByLevel2InviterIdOrderByIdDesc(Long level2InviterId);

    List<DistributionRelation> findByLevel3InviterIdOrderByIdDesc(Long level3InviterId);
}
