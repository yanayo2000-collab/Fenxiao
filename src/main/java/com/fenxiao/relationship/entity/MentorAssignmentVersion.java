package com.fenxiao.relationship.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.relationship.domain.MentorAssignmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="mentor_assignment_version")
public class MentorAssignmentVersion extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="student_user_id",nullable=false) private Long studentUserId;
    @Column(name="mentor_user_id") private Long mentorUserId;
    @Enumerated(EnumType.STRING) @Column(name="assignment_status",nullable=false) private MentorAssignmentStatus assignmentStatus;
    @Column(name="version_no",nullable=false) private int versionNo;
    @Column(name="effective_from",nullable=false) private LocalDateTime effectiveFrom;
    @Column(name="effective_to") private LocalDateTime effectiveTo;
    @Column(name="change_reason",nullable=false) private String changeReason;
    @Column(name="source_type",nullable=false) private String sourceType;
    @Column(name="source_reference") private String sourceReference;
    @Column(name="operated_by") private Long operatedBy;
    protected MentorAssignmentVersion(){}
    public static MentorAssignmentVersion pending(Long student,int version,LocalDateTime from){return create(student,null,MentorAssignmentStatus.MENTOR_ASSIGNMENT_PENDING,version,from,"NO_QUALIFIED_MENTOR","SYSTEM",null,null);}
    public static MentorAssignmentVersion assigned(Long student,Long mentor,int version,LocalDateTime from,String reason,String source,String reference,Long operator){return create(student,mentor,MentorAssignmentStatus.ASSIGNED,version,from,reason,source,reference,operator);}
    private static MentorAssignmentVersion create(Long student,Long mentor,MentorAssignmentStatus status,int version,LocalDateTime from,String reason,String source,String reference,Long operator){var v=new MentorAssignmentVersion();v.studentUserId=student;v.mentorUserId=mentor;v.assignmentStatus=status;v.versionNo=version;v.effectiveFrom=from;v.changeReason=reason;v.sourceType=source;v.sourceReference=reference;v.operatedBy=operator;return v;}
    public Long getStudentUserId(){return studentUserId;} public Long getMentorUserId(){return mentorUserId;} public MentorAssignmentStatus getAssignmentStatus(){return assignmentStatus;} public int getVersionNo(){return versionNo;} public LocalDateTime getEffectiveFrom(){return effectiveFrom;} public LocalDateTime getEffectiveTo(){return effectiveTo;}
    public void endAt(LocalDateTime at){if(effectiveTo!=null)throw new IllegalStateException("mentor assignment already ended");effectiveTo=at;assignmentStatus=MentorAssignmentStatus.ENDED;}
}
