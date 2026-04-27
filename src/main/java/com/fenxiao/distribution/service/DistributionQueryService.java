package com.fenxiao.distribution.service;

import com.fenxiao.admin.api.dto.RelationDetailResponse;
import com.fenxiao.admin.service.AdminProductScopeService;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DistributionQueryService {

    private final DistributionRelationRepository distributionRelationRepository;
    private final AdminProductScopeService adminProductScopeService;

    public DistributionQueryService(DistributionRelationRepository distributionRelationRepository,
                                    AdminProductScopeService adminProductScopeService) {
        this.distributionRelationRepository = distributionRelationRepository;
        this.adminProductScopeService = adminProductScopeService;
    }

    public RelationDetailResponse getRelationDetail(Long userId) {
        return getRelationDetail(userId, null);
    }

    public RelationDetailResponse getRelationDetail(Long userId, String productCode) {
        String normalizedProductCode = adminProductScopeService.normalizeProductCode(productCode);
        if (normalizedProductCode != null) {
            List<Long> scopedUserIds = adminProductScopeService.resolveScopedUserIds(normalizedProductCode);
            if (!scopedUserIds.contains(userId)) {
                throw new IllegalArgumentException("distribution relation not found for product");
            }
        }
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
