package com.fenxiao.identity;

import com.fenxiao.common.api.ForbiddenException;
import com.fenxiao.distribution.service.DistributionBindingService;
import com.fenxiao.identity.domain.AccountStatus;
import com.fenxiao.identity.service.AccountLifecycleService;
import com.fenxiao.identity.service.UserSessionService;
import com.fenxiao.relationship.domain.MentorAssignmentStatus;
import com.fenxiao.relationship.repository.InvitationRelationVersionRepository;
import com.fenxiao.relationship.repository.MentorAssignmentVersionRepository;
import com.fenxiao.relationship.repository.OperatingTeamRepository;
import com.fenxiao.relationship.repository.TeamMembershipVersionRepository;
import com.fenxiao.relationship.service.RelationshipFoundationService;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class IdentityRelationshipFoundationTest {
    @Autowired DistributionBindingService bindingService;
    @Autowired RelationshipFoundationService relationshipService;
    @Autowired InvitationRelationVersionRepository invitationRepository;
    @Autowired MentorAssignmentVersionRepository mentorRepository;
    @Autowired TeamMembershipVersionRepository teamMembershipRepository;
    @Autowired OperatingTeamRepository teamRepository;
    @Autowired UserSessionService sessionService;
    @Autowired AccountLifecycleService accountLifecycleService;
    @Autowired UserDistributionProfileRepository profileRepository;

    @Test
    void shouldInitializeIndependentInvitationMentorAndTeamRelationships() {
        var root = bindingService.createProfile(70100L, "BR", "pt-br", null);
        var child = bindingService.createProfile(70101L, "BR", "pt-br", root.getInviteCode());

        var invitation = invitationRepository.findTopByUserIdOrderByVersionNoDesc(child.getUserId()).orElseThrow();
        var mentor = mentorRepository.findTopByStudentUserIdOrderByVersionNoDesc(child.getUserId()).orElseThrow();
        var team = teamMembershipRepository.findTopByUserIdOrderByVersionNoDesc(child.getUserId()).orElseThrow();

        assertThat(invitation.getInviterUserId()).isEqualTo(root.getUserId());
        assertThat(mentor.getAssignmentStatus()).isEqualTo(MentorAssignmentStatus.MENTOR_ASSIGNMENT_PENDING);
        assertThat(teamRepository.findById(team.getTeamId()).orElseThrow().getTeamCode()).isEqualTo("SYSTEM-UNASSIGNED-BR");
    }

    @Test
    void shouldAutoAssignQualifiedMentorAndVersionTeamTransfer() {
        var root = bindingService.createProfile(70200L, "BR", "pt-br", null);
        var mentorUser = bindingService.createProfile(70201L, "BR", "pt-br", root.getInviteCode());
        relationshipService.qualifyMentor(mentorUser.getUserId(), "BR", "pt-br", 2);
        var student = bindingService.createProfile(70202L, "BR", "pt-br", root.getInviteCode());

        assertThat(mentorRepository.findTopByStudentUserIdOrderByVersionNoDesc(student.getUserId()).orElseThrow().getMentorUserId())
                .isEqualTo(mentorUser.getUserId());

        var target = relationshipService.createTeam("BR-SP-01", "Sao Paulo 01", "BR", null);
        relationshipService.transferTeam(student.getUserId(), target.getId(), null, "OPERATIONS_ASSIGNMENT", 1L);
        var latest = teamMembershipRepository.findTopByUserIdOrderByVersionNoDesc(student.getUserId()).orElseThrow();
        assertThat(latest.getTeamId()).isEqualTo(target.getId());
        assertThat(latest.getVersionNo()).isEqualTo(2);
    }

    @Test
    void shouldRevokeSessionImmediatelyWhenAccountIsFrozenAndKeepCancellationPermanent() {
        var root = bindingService.createProfile(70300L, "BR", "pt-br", null);
        var user = bindingService.createProfile(70301L, "BR", "pt-br", root.getInviteCode());
        var session = sessionService.issue(user.getUserId());
        assertThat(sessionService.assertAccess(user.getUserId(), session.accessToken())).isEqualTo(user.getUserId());

        accountLifecycleService.change(user.getUserId(), "FREEZE", "risk review", 1L, "super_admin", "127.0.0.1");
        assertThatThrownBy(() -> sessionService.assertAccess(user.getUserId(), session.accessToken())).isInstanceOf(ForbiddenException.class);

        accountLifecycleService.change(user.getUserId(), "CANCEL", "user cancellation", 1L, "super_admin", "127.0.0.1");
        assertThat(profileRepository.findById(user.getUserId()).orElseThrow().getAccountStatus()).isEqualTo(AccountStatus.CANCELLED);
        assertThatThrownBy(() -> accountLifecycleService.change(user.getUserId(), "ACTIVATE", "not allowed", 1L, "super_admin", "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class).hasMessage("cancelled account is permanent");
    }
}
