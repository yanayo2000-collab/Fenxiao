package com.fenxiao.admin.api;

import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.service.RewardCalculationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/distribution")
public class DistributionAdminController {

    private final RewardCalculationService rewardCalculationService;

    public DistributionAdminController(RewardCalculationService rewardCalculationService) {
        this.rewardCalculationService = rewardCalculationService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "module", "distribution-admin",
                "status", "ok",
                "phase", "mvp-bootstrap"
        );
    }

    @GetMapping("/rewards")
    public RewardListResponse listRewards() {
        return rewardCalculationService.getRecentRewards();
    }
}
