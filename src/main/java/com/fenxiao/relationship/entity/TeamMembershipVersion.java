package com.fenxiao.relationship.entity;

import com.fenxiao.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="team_membership_version")
public class TeamMembershipVersion extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="team_id",nullable=false) private Long teamId;
    @Column(name="version_no",nullable=false) private int versionNo;
    @Column(name="effective_from",nullable=false) private LocalDateTime effectiveFrom;
    @Column(name="effective_to") private LocalDateTime effectiveTo;
    @Column(name="change_reason",nullable=false) private String changeReason;
    @Column(name="source_type",nullable=false) private String sourceType;
    @Column(name="source_reference") private String sourceReference;
    @Column(name="operated_by") private Long operatedBy;
    protected TeamMembershipVersion(){}
    public static TeamMembershipVersion create(Long user,Long team,int version,LocalDateTime from,String reason,String source,String reference,Long operator){var v=new TeamMembershipVersion();v.userId=user;v.teamId=team;v.versionNo=version;v.effectiveFrom=from;v.changeReason=reason;v.sourceType=source;v.sourceReference=reference;v.operatedBy=operator;return v;}
    public Long getUserId(){return userId;} public Long getTeamId(){return teamId;} public int getVersionNo(){return versionNo;} public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public LocalDateTime getEffectiveTo(){return effectiveTo;}
    public void endAt(LocalDateTime at){if(effectiveTo!=null)throw new IllegalStateException("team membership already ended");effectiveTo=at;}
}
