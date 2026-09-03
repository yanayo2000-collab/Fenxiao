package com.fenxiao.relationship.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invitation_relation_version")
public class InvitationRelationVersion extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "inviter_user_id") private Long inviterUserId;
    @Column(name = "version_no", nullable = false) private int versionNo;
    @Column(name = "effective_from", nullable = false) private LocalDateTime effectiveFrom;
    @Column(name = "effective_to") private LocalDateTime effectiveTo;
    @Column(name = "change_reason", nullable = false) private String changeReason;
    @Column(name = "source_type", nullable = false) private String sourceType;
    @Column(name = "source_reference") private String sourceReference;
    @Column(name = "operated_by") private Long operatedBy;
    protected InvitationRelationVersion() {}
    public static InvitationRelationVersion create(Long userId, Long inviterUserId, int versionNo, LocalDateTime from, String reason, String sourceType, String reference, Long operatedBy) {
        var value = new InvitationRelationVersion(); value.userId=userId; value.inviterUserId=inviterUserId; value.versionNo=versionNo; value.effectiveFrom=from; value.changeReason=reason; value.sourceType=sourceType; value.sourceReference=reference; value.operatedBy=operatedBy; return value;
    }
    public Long getUserId(){return userId;} public Long getInviterUserId(){return inviterUserId;} public int getVersionNo(){return versionNo;} public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public LocalDateTime getEffectiveTo(){return effectiveTo;}
    public void endAt(LocalDateTime at){ if(effectiveTo!=null) throw new IllegalStateException("invitation relation version already ended"); effectiveTo=at; }
}
