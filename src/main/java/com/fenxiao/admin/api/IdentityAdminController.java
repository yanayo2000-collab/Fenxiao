package com.fenxiao.admin.api;

import com.fenxiao.admin.api.dto.AccountStatusChangeRequest;
import com.fenxiao.admin.api.dto.MentorAssignmentRequest;
import com.fenxiao.admin.api.dto.TeamTransferRequest;
import com.fenxiao.admin.api.dto.MentorQualificationRequest;
import com.fenxiao.admin.api.dto.CreateTeamRequest;
import com.fenxiao.common.security.DistributionAccessGuard;
import com.fenxiao.identity.service.AccountLifecycleService;
import com.fenxiao.relationship.service.RelationshipFoundationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/identity")
public class IdentityAdminController {
    private final DistributionAccessGuard accessGuard;
    private final AccountLifecycleService accountLifecycleService;
    private final RelationshipFoundationService relationshipService;

    public IdentityAdminController(DistributionAccessGuard accessGuard,
                                   AccountLifecycleService accountLifecycleService,
                                   RelationshipFoundationService relationshipService) {
        this.accessGuard = accessGuard;
        this.accountLifecycleService = accountLifecycleService;
        this.relationshipService = relationshipService;
    }

    @PostMapping("/users/{userId}/account-status")
    public Map<String,Object> changeAccountStatus(@RequestHeader(value="X-Admin-Token",required=false) String adminToken,
                                                   @RequestHeader(value="X-Admin-Session",required=false) String session,
                                                   @PathVariable Long userId,
                                                   @Valid @RequestBody AccountStatusChangeRequest request,
                                                   HttpServletRequest httpRequest) {
        var principal = accessGuard.assertAdminAccountManageAccess(adminToken, session);
        var profile = accountLifecycleService.change(userId, request.action(), request.reason(), principal.accountId(), principal.role(), httpRequest.getRemoteAddr());
        return Map.of("userId", userId, "accountStatus", profile.getAccountStatus().name());
    }

    @PostMapping("/users/{userId}/mentor")
    public Map<String,Object> assignMentor(@RequestHeader(value="X-Admin-Token",required=false) String adminToken,
                                           @RequestHeader(value="X-Admin-Session",required=false) String session,
                                           @PathVariable Long userId,
                                           @Valid @RequestBody MentorAssignmentRequest request) {
        var principal = accessGuard.assertMentorManageAccess(adminToken, session);
        var assignment = relationshipService.assignMentor(userId, request.mentorUserId(), request.reason(), principal.accountId());
        return Map.of("userId", userId, "mentorUserId", assignment.getMentorUserId(), "status", assignment.getAssignmentStatus().name(), "version", assignment.getVersionNo());
    }

    @PostMapping("/mentors/{userId}/qualification")
    public Map<String,Object> qualifyMentor(@RequestHeader(value="X-Admin-Token",required=false) String adminToken,
                                            @RequestHeader(value="X-Admin-Session",required=false) String session,
                                            @PathVariable Long userId,
                                            @Valid @RequestBody MentorQualificationRequest request) {
        accessGuard.assertMentorManageAccess(adminToken, session);
        var mentor = relationshipService.qualifyMentor(userId, request.countryCode(), request.languageCode(), request.maxActiveStudents());
        return Map.of("userId", userId, "status", mentor.getQualificationStatus().name(), "maxActiveStudents", mentor.getMaxActiveStudents());
    }

    @PostMapping("/teams")
    public Map<String,Object> createTeam(@RequestHeader(value="X-Admin-Token",required=false) String adminToken,
                                         @RequestHeader(value="X-Admin-Session",required=false) String session,
                                         @Valid @RequestBody CreateTeamRequest request) {
        accessGuard.assertTeamManageAccess(adminToken, session);
        var team = relationshipService.createTeam(request.teamCode(), request.teamName(), request.countryCode(), request.leaderUserId());
        return Map.of("teamId", team.getId(), "teamCode", team.getTeamCode(), "countryCode", team.getCountryCode());
    }

    @PostMapping("/users/{userId}/team")
    public Map<String,Object> transferTeam(@RequestHeader(value="X-Admin-Token",required=false) String adminToken,
                                           @RequestHeader(value="X-Admin-Session",required=false) String session,
                                           @PathVariable Long userId,
                                           @Valid @RequestBody TeamTransferRequest request) {
        var principal = accessGuard.assertTeamManageAccess(adminToken, session);
        var membership = relationshipService.transferTeam(userId, request.teamId(), request.effectiveAt(), request.reason(), principal.accountId());
        return Map.of("userId", userId, "teamId", membership.getTeamId(), "version", membership.getVersionNo(), "effectiveAt", membership.getEffectiveFrom().toString());
    }
}
