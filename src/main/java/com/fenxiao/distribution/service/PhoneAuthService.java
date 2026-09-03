package com.fenxiao.distribution.service;

import com.fenxiao.common.api.TooManyRequestsException;
import com.fenxiao.distribution.api.dto.PhoneLoginRequest;
import com.fenxiao.distribution.entity.PhoneVerificationCode;
import com.fenxiao.distribution.repository.PhoneVerificationCodeRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import com.fenxiao.identity.service.UserSessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Locale;

@Service
@Transactional
public class PhoneAuthService {
    private static final String PURPOSE = "LOGIN";
    private static final int MAX_ATTEMPTS = 5;
    private static final int TTL_MINUTES = 10;
    private final PhoneVerificationCodeRepository codeRepository;
    private final UserDistributionProfileRepository profileRepository;
    private final DistributionBindingService bindingService;
    private final SmsSender smsSender;
    private final UserSessionService userSessionService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public PhoneAuthService(PhoneVerificationCodeRepository codeRepository,
                            UserDistributionProfileRepository profileRepository,
                            DistributionBindingService bindingService,
                            SmsSender smsSender,
                            UserSessionService userSessionService) {
        this(codeRepository, profileRepository, bindingService, smsSender, userSessionService, Clock.systemUTC());
    }

    PhoneAuthService(PhoneVerificationCodeRepository codeRepository,
                     UserDistributionProfileRepository profileRepository,
                     DistributionBindingService bindingService,
                     SmsSender smsSender,
                     UserSessionService userSessionService,
                     Clock clock) {
        this.codeRepository = codeRepository;
        this.profileRepository = profileRepository;
        this.bindingService = bindingService;
        this.smsSender = smsSender;
        this.userSessionService = userSessionService;
        this.clock = clock;
    }

    public String issueCode(String phoneNumber) {
        String normalizedPhone = normalizePhone(phoneNumber);
        LocalDateTime now = LocalDateTime.now(clock);
        codeRepository.findTopByPhoneNumberAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByIdDesc(normalizedPhone, PURPOSE, now)
                .ifPresent(existing -> {
                    throw new TooManyRequestsException("phone verification code already sent, please retry later");
                });
        String code = String.format("%06d", random.nextInt(1_000_000));
        codeRepository.save(PhoneVerificationCode.issue(normalizedPhone, code, PURPOSE, now.plusMinutes(TTL_MINUTES)));
        smsSender.sendVerificationCode(normalizedPhone, code, TTL_MINUTES);
        return code;
    }

    public LoginResult login(PhoneLoginRequest request) {
        String normalizedPhone = normalizePhone(request.phoneNumber());
        boolean existingUser = profileRepository.findByPhoneNumber(normalizedPhone).isPresent();
        if (!existingUser) {
            String countryCode = defaultIfBlank(request.countryCode(), "BR").toUpperCase(Locale.ROOT);
            if ("BR".equals(countryCode) && !normalizedPhone.startsWith("+55")) {
                throw new IllegalArgumentException("Brazil registration requires a +55 phone number");
            }
            if (request.inviteCode() == null || request.inviteCode().isBlank()) {
                throw new IllegalArgumentException("valid invite code is required for registration");
            }
            if (profileRepository.findByInviteCode(request.inviteCode().trim().toUpperCase(Locale.ROOT)).isEmpty()) {
                throw new IllegalArgumentException("invite code not found");
            }
        }
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
        UserDistributionProfile profile = profileRepository.findByPhoneNumber(normalizedPhone).orElseGet(() -> {
            long generatedUserId = 9000000000L + Math.abs((long) normalizedPhone.hashCode());
            while (profileRepository.existsById(generatedUserId)) generatedUserId++;
            UserDistributionProfile createdProfile = bindingService.createProfile(
                    generatedUserId,
                    defaultIfBlank(request.countryCode(), "BR").toUpperCase(Locale.ROOT),
                    defaultIfBlank(request.languageCode(), "pt-BR").toLowerCase(Locale.ROOT),
                    request.inviteCode());
            createdProfile.bindPhoneNumber(normalizedPhone);
            return profileRepository.save(createdProfile);
        });
        UserSessionService.IssuedSession session = userSessionService.issue(profile.getUserId());
        return new LoginResult(profile, session);
    }

    private String normalizePhone(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.replaceAll("[\\s()-]", "").trim();
        if (!normalized.matches("^\\+?[0-9]{6,20}$")) throw new IllegalArgumentException("phone number is invalid");
        return normalized;
    }
    private String defaultIfBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    public record LoginResult(UserDistributionProfile profile, UserSessionService.IssuedSession session) {}
}
