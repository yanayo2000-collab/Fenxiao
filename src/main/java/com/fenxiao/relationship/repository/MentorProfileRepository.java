package com.fenxiao.relationship.repository;

import com.fenxiao.relationship.domain.MentorQualificationStatus;
import com.fenxiao.relationship.entity.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MentorProfileRepository extends JpaRepository<MentorProfile,Long> {
    List<MentorProfile> findByQualificationStatusAndCountryCodeAndLanguageCodeOrderByUserIdAsc(MentorQualificationStatus status,String countryCode,String languageCode);
}
