package com.fenxiao.relationship.service;

import com.fenxiao.relationship.domain.MentorQualificationStatus;
import com.fenxiao.relationship.entity.*;
import com.fenxiao.relationship.repository.*;
import com.fenxiao.user.entity.UserDistributionProfile;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class RelationshipFoundationService {
    private final InvitationRelationVersionRepository invitationRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorAssignmentVersionRepository mentorAssignmentRepository;
    private final OperatingTeamRepository teamRepository;
    private final TeamMembershipVersionRepository teamMembershipRepository;
    private final Clock clock;

    public RelationshipFoundationService(InvitationRelationVersionRepository invitationRepository,
                                         MentorProfileRepository mentorProfileRepository,
                                         MentorAssignmentVersionRepository mentorAssignmentRepository,
                                         OperatingTeamRepository teamRepository,
                                         TeamMembershipVersionRepository teamMembershipRepository,
                                         Clock clock) {
        this.invitationRepository = invitationRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorAssignmentRepository = mentorAssignmentRepository;
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.clock = clock;
    }

    public void initializeForRegistration(UserDistributionProfile user, Long inviterUserId, String inviteCode) {
        LocalDateTime now = LocalDateTime.now(clock);
        invitationRepository.findTopByUserIdOrderByVersionNoDesc(user.getUserId()).orElseGet(() ->
                invitationRepository.save(InvitationRelationVersion.create(
                        user.getUserId(), inviterUserId, 1, now, "REGISTRATION", "INVITE_CODE", inviteCode, null)));
        initializeMentor(user, now);
        initializeHoldingTeam(user, now);
    }

    public MentorAssignmentVersion assignMentor(Long studentUserId, Long mentorUserId, String reason, Long operatorId) {
        MentorProfile mentor = mentorProfileRepository.findById(mentorUserId)
                .filter(value -> value.getQualificationStatus() == MentorQualificationStatus.QUALIFIED)
                .orElseThrow(() -> new IllegalArgumentException("mentor is not qualified"));
        long load = mentorAssignmentRepository.countByMentorUserIdAndEffectiveToIsNull(mentorUserId);
        if (load >= mentor.getMaxActiveStudents()) throw new IllegalStateException("mentor capacity exceeded");
        LocalDateTime now = LocalDateTime.now(clock);
        MentorAssignmentVersion current = mentorAssignmentRepository.findTopByStudentUserIdOrderByVersionNoDesc(studentUserId).orElse(null);
        int version = current == null ? 1 : current.getVersionNo() + 1;
        if (current != null && current.getEffectiveTo() == null) current.endAt(now);
        return mentorAssignmentRepository.save(MentorAssignmentVersion.assigned(studentUserId, mentorUserId, version, now, normalizeReason(reason), "ADMIN", null, operatorId));
    }

    public MentorProfile qualifyMentor(Long userId, String countryCode, String languageCode, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("mentor capacity must be positive");
        MentorProfile profile = mentorProfileRepository.findById(userId)
                .orElseGet(() -> MentorProfile.qualified(userId, countryCode.trim().toUpperCase(Locale.ROOT), languageCode.trim().toLowerCase(Locale.ROOT), capacity));
        profile.qualify(countryCode.trim().toUpperCase(Locale.ROOT), languageCode.trim().toLowerCase(Locale.ROOT), capacity);
        return mentorProfileRepository.save(profile);
    }

    public OperatingTeam createTeam(String code, String name, String countryCode, Long leaderUserId) {
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        if (teamRepository.findByTeamCode(normalizedCode).isPresent()) throw new IllegalStateException("team code already exists");
        return teamRepository.save(OperatingTeam.create(normalizedCode, name.trim(), countryCode.trim().toUpperCase(Locale.ROOT), leaderUserId));
    }

    public TeamMembershipVersion transferTeam(Long userId, Long teamId, LocalDateTime effectiveAt, String reason, Long operatorId) {
        teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("team not found"));
        LocalDateTime at = effectiveAt == null ? LocalDateTime.now(clock) : effectiveAt;
        TeamMembershipVersion current = teamMembershipRepository.findTopByUserIdOrderByVersionNoDesc(userId).orElse(null);
        if (current != null && at.isBefore(current.getEffectiveFrom())) {
            throw new IllegalArgumentException("team transfer cannot predate the current membership");
        }
        int version = current == null ? 1 : current.getVersionNo() + 1;
        if (current != null && current.getEffectiveTo() == null) current.endAt(at);
        return teamMembershipRepository.save(TeamMembershipVersion.create(userId, teamId, version, at, normalizeReason(reason), "ADMIN", null, operatorId));
    }

    private void initializeMentor(UserDistributionProfile user, LocalDateTime now) {
        if (mentorAssignmentRepository.findTopByStudentUserIdOrderByVersionNoDesc(user.getUserId()).isPresent()) return;
        var candidates = mentorProfileRepository.findByQualificationStatusAndCountryCodeAndLanguageCodeOrderByUserIdAsc(
                MentorQualificationStatus.QUALIFIED, user.getCountryCode(), user.getLanguageCode());
        for (MentorProfile mentor : candidates) {
            if (mentorAssignmentRepository.countByMentorUserIdAndEffectiveToIsNull(mentor.getUserId()) < mentor.getMaxActiveStudents()) {
                mentorAssignmentRepository.save(MentorAssignmentVersion.assigned(user.getUserId(), mentor.getUserId(), 1, now, "AUTO_MATCH", "SYSTEM", null, null));
                return;
            }
        }
        mentorAssignmentRepository.save(MentorAssignmentVersion.pending(user.getUserId(), 1, now));
    }

    private void initializeHoldingTeam(UserDistributionProfile user, LocalDateTime now) {
        if (teamMembershipRepository.findTopByUserIdOrderByVersionNoDesc(user.getUserId()).isPresent()) return;
        String country = user.getCountryCode().toUpperCase(Locale.ROOT);
        String code = "SYSTEM-UNASSIGNED-" + country;
        OperatingTeam team = teamRepository.findByTeamCode(code)
                .orElseGet(() -> teamRepository.save(OperatingTeam.create(code, "Awaiting team assignment (" + country + ")", country, null)));
        teamMembershipRepository.save(TeamMembershipVersion.create(user.getUserId(), team.getId(), 1, now, "REGISTRATION_HOLDING_TEAM", "SYSTEM", null, null));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("change reason is required");
        return reason.trim();
    }
}
