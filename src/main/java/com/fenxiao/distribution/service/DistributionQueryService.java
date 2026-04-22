package com.fenxiao.distribution.service;

import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DistributionQueryService {

    private final DistributionRelationRepository distributionRelationRepository;

    public DistributionQueryService(DistributionRelationRepository distributionRelationRepository) {
        this.distributionRelationRepository = distributionRelationRepository;
    }

    public RelationDetailResponse getRelationDetail(Long userId) {
        DistributionRelation relation = distributionRelationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));
        return toDetailResponse(relation);
    }

    public static RelationDetailResponse toDetailResponse(DistributionRelation relation) {
        return new RelationDetailResponse(
                relation.getUserId(),
                relation.getLevel1InviterId(),
                relation.getLevel2InviterId(),
                relation.getLevel3InviterId(),
                relation.getBindSource(),
                relation.getLockStatus(),
                relation.getBindTime(),
                relation.getLockTime(),
                relation.getCountryCode(),
                relation.isCrossCountry()
        );
    }
}
