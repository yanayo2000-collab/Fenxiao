package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.PhoneVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PhoneVerificationCodeRepository extends JpaRepository<PhoneVerificationCode, Long> {
    Optional<PhoneVerificationCode> findTopByPhoneNumberAndPurposeAndConsumedFalseOrderByIdDesc(String phoneNumber, String purpose);
}
