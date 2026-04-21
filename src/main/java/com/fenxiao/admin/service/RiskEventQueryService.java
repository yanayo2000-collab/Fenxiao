package com.fenxiao.admin.service;

import com.fenxiao.admin.api.dto.RiskEventListItem;
import com.fenxiao.admin.api.dto.RiskEventListResponse;
import com.fenxiao.risk.domain.RiskStatus;
import com.fenxiao.risk.entity.RiskEvent;
import com.fenxiao.risk.repository.RiskEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEventQueryService {

    private final RiskEventRepository riskEventRepository;

    public RiskEventQueryService(RiskEventRepository riskEventRepository) {
        this.riskEventRepository = riskEventRepository;
    }

    public RiskEventListResponse getRiskEvents(Long userId,
                                               RiskStatus riskStatus,
                                               LocalDateTime startAt,
                                               LocalDateTime endAt,
                                               int page,
                                               int size) {
        validatePageRequest(page, size);
        Page<RiskEvent> riskEvents = riskEventRepository.findAdminRiskEvents(
                userId,
                riskStatus,
                startAt,
                endAt,
                PageRequest.of(page, size)
        );

        List<RiskEventListItem> items = new ArrayList<>();
        for (RiskEvent riskEvent : riskEvents.getContent()) {
            items.add(new RiskEventListItem(
                    riskEvent.getId(),
                    riskEvent.getUserId(),
                    riskEvent.getRiskType(),
                    riskEvent.getRiskLevel(),
                    riskEvent.getRiskStatus(),
                    riskEvent.getDetailJson(),
                    riskEvent.getDetectedAt()
            ));
        }
        return new RiskEventListResponse(items, riskEvents.getTotalElements(), page, size);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
