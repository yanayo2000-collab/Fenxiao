package com.fenxiao.relationship.repository;

import com.fenxiao.relationship.entity.InvitationRelationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface InvitationRelationVersionRepository extends JpaRepository<InvitationRelationVersion,Long> {
    Optional<InvitationRelationVersion> findTopByUserIdOrderByVersionNoDesc(Long userId);
    List<InvitationRelationVersion> findByInviterUserIdAndEffectiveToIsNull(Long inviterUserId);
}
