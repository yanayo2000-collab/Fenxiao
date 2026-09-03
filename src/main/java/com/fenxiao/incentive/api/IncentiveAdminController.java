package com.fenxiao.incentive.api;

import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.incentive.dto.*;
import com.fenxiao.incentive.service.IncentiveShadowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class IncentiveAdminController {
    private final DistributionAccessGuard guard;
    private final IncentiveShadowService service;
    public IncentiveAdminController(DistributionAccessGuard guard, IncentiveShadowService service) { this.guard = guard; this.service = service; }

    @PostMapping("/admin/incentives/mentor-rules")
    public Map<String,Object> mentorRule(@RequestHeader(value="X-Admin-Token",required=false) String token,
                                         @RequestHeader(value="X-Admin-Session",required=false) String session,
                                         @Valid @RequestBody MentorRewardRuleRequest request) {
        guard.assertAdminWriteAccess(token, session); return Map.of("ruleId", service.configureMentorRule(request), "ledgerMode", "SHADOW");
    }
    @PostMapping("/admin/incentives/leadership-policies")
    public Map<String,Object> leadershipPolicy(@RequestHeader(value="X-Admin-Token",required=false) String token,
                                               @RequestHeader(value="X-Admin-Session",required=false) String session,
                                               @Valid @RequestBody LeadershipPolicyRequest request) {
        guard.assertAdminWriteAccess(token, session); return Map.of("policyId", service.configureLeadershipPolicy(request), "ledgerMode", "SHADOW");
    }
    @GetMapping("/admin/incentives/shadow-report")
    public Map<String,Long> report(@RequestHeader(value="X-Admin-Token",required=false) String token,
                                   @RequestHeader(value="X-Admin-Session",required=false) String session) {
        guard.assertAdminAccess(token, session); return service.shadowCounts();
    }
    @PostMapping("/internal/distribution/team-profit-facts")
    public IncentiveShadowService.TeamProfitResult teamProfit(@RequestHeader(value="X-Internal-Token",required=false) String token,
                                                               @Valid @RequestBody TeamProfitFactRequest request) {
        guard.assertInternalToken(token); return service.ingestTeamProfit(request);
    }
}
