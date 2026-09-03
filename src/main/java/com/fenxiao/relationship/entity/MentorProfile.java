package com.fenxiao.relationship.entity;

import com.fenxiao.common.entity.BaseEntity;
import com.fenxiao.relationship.domain.MentorQualificationStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "mentor_profile")
public class MentorProfile extends BaseEntity {
    @Id @Column(name="user_id") private Long userId;
    @Column(name="country_code", nullable=false) private String countryCode;
    @Column(name="language_code", nullable=false) private String languageCode;
    @Enumerated(EnumType.STRING) @Column(name="qualification_status", nullable=false) private MentorQualificationStatus qualificationStatus;
    @Column(name="max_active_students", nullable=false) private int maxActiveStudents;
    protected MentorProfile(){}
    public static MentorProfile qualified(Long userId,String country,String language,int capacity){var p=new MentorProfile();p.userId=userId;p.countryCode=country;p.languageCode=language;p.maxActiveStudents=capacity;p.qualificationStatus=MentorQualificationStatus.QUALIFIED;return p;}
    public Long getUserId(){return userId;} public String getCountryCode(){return countryCode;} public String getLanguageCode(){return languageCode;} public int getMaxActiveStudents(){return maxActiveStudents;} public MentorQualificationStatus getQualificationStatus(){return qualificationStatus;}
    public void qualify(String country,String language,int capacity){if(capacity<1)throw new IllegalArgumentException("mentor capacity must be positive");countryCode=country;languageCode=language;maxActiveStudents=capacity;qualificationStatus=MentorQualificationStatus.QUALIFIED;}
}
