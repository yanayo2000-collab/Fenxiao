package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.PhoneLoginRequest;
import com.fenxiao.distribution.entity.PhoneVerificationCode;
import com.fenxiao.distribution.repository.PhoneVerificationCodeRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
public class PhoneAuthService {
    private static final String PURPOSE = "LOGIN";
    private static final int MAX_ATTEMPTS = 5;
    private final PhoneVerificationCodeRepository codeRepository;
    private final UserDistributionProfileRepository profileRepository;
    private final DistributionBindingService bindingService;
    private final Clock clock;

    @Autowired
    public PhoneAuthService(PhoneVerificationCodeRepository codeRepository,
                            UserDistributionProfileRepository profileRepository,
                            DistributionBindingService bindingService) {
        this(codeRepository, profileRepository, bindingService, Clock.systemUTC());
    }

    PhoneAuthService(PhoneVerificationCodeRepository codeRepository,
                     UserDistributionProfileRepository profileRepository,
                     DistributionBindingService bindingService,
                     Clock clock) {
        this.codeRepository = codeRepository;
        this.profileRepository = profileRepository;
        this.bindingService = bindingService;
        this.clock = clock;
    }

    public String issueCode(String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        String code = "246810";
        codeRepository.save(PhoneVerificationCode.issue(normalizedPhone, code, PURPOSE, LocalDateTime.now(clock).plusMinutes(10)));
        return code;
    }

    public UserDistributionProfile login(PhoneLoginRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());
        PhoneVerificationCode code = codeRepository.findTopByPhoneNumberAndPurposeAndConsumedFalseOrderByIdDesc(normalizedPhone, PURPOSE)
                .orElseThrow(() -> new IllegalArgumentException("verification code not found"));
        if (code.expired(clock)) throw new IllegalStateException("verification code expired");
        if (code.getAttempts() >= MAX_ATTEMPTS) throw new IllegalStateException("verification attempts exceeded");
        if (!code.getVerificationCode().equals(request.verificationCode().trim())) {
            code.failAttempt();
            codeRepository.save(code);
            throw new IllegalArgumentException("verification code invalid");
        }
        code.consume();
        codeRepository.save(code);
        return profileRepository.findByPhoneNumber(normalizedPhone).orElseGet(() -> {
            long generatedUserId = 9000000000L + Math.abs((long) normalizedPhone.hashCode());
            while (profileRepository.existsById(generatedUserId)) generatedUserId++;
            UserDistributionProfile profile = bindingService.createProfile(
                    generatedUserId,
                    defaultIfBlank(request.countryCode(), "ID").toUpperCase(Locale.ROOT),
                    defaultIfBlank(request.languageCode(), "id"),
                    request.inviteCode());
            profile.bindPhoneNumber(normalizedPhone);
            return profileRepository.save(profile);
        });
    }

    private String normalizePhone(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.replaceAll("[\\s()-]", "").trim();
        if (!normalized.matches("^\\+?[0-9]{6,20}$")) throw new IllegalArgumentException("phone number is invalid");
        return normalized;
    }
    private String defaultIfBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
}
