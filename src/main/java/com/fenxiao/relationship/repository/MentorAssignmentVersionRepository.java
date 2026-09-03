package com.fenxiao.relationship.repository;

import com.fenxiao.relationship.entity.MentorAssignmentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MentorAssignmentVersionRepository extends JpaRepository<MentorAssignmentVersion,Long> {
    Optional<MentorAssignmentVersion> findTopByStudentUserIdOrderByVersionNoDesc(Long studentUserId);
    long countByMentorUserIdAndEffectiveToIsNull(Long mentorUserId);
}
