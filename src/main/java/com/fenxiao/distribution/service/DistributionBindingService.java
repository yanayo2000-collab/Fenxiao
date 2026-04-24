package com.fenxiao.distribution.service;

import com.fenxiao.distribution.domain.BindSource;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

@Service
@Transactional
public class DistributionBindingService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXY3456789";
    private static final int INVITE_CODE_LENGTH = 8;

    private final UserDistributionProfileRepository userProfileRepository;
    private final DistributionRelationRepository relationRepository;
    private final Random random = new Random();

    public DistributionBindingService(UserDistributionProfileRepository userProfileRepository,
                                      DistributionRelationRepository relationRepository) {
        this.userProfileRepository = userProfileRepository;
        this.relationRepository = relationRepository;
    }

    public UserDistributionProfile createProfile(Long userId, String countryCode, String languageCode, String inviteCode) {
        if (userProfileRepository.existsById(userId)) {
            throw new IllegalStateException("user profile already exists");
        }

        UserDistributionProfile profile = UserDistributionProfile.create(
                userId,
                normalizeCountry(countryCode),
                languageCode.trim().toLowerCase(Locale.ROOT),
                generateUniqueInviteCode()
        );
        userProfileRepository.save(profile);

        if (inviteCode == null || inviteCode.isBlank()) {
            relationRepository.save(DistributionRelation.createRoot(userId, profile.getCountryCode()));
        } else {
            bindInviter(userId, inviteCode);
        }
        return profile;
    }

    public UserDistributionProfile ensureRootProfile(Long userId, String countryCode, String languageCode) {
        return userProfileRepository.findById(userId)
                .orElseGet(() -> createProfile(userId, countryCode, languageCode, null));
    }

    public DistributionRelation bindInviter(Long userId, String inviteCode) {
        UserDistributionProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user profile not found"));

        if (relationRepository.existsByUserId(userId)) {
            throw new IllegalStateException("user already has inviter");
        }

        UserDistributionProfile inviter = userProfileRepository.findByInviteCode(normalizeInviteCode(inviteCode))
                .orElseThrow(() -> new IllegalArgumentException("invite code not found"));

        if (Objects.equals(inviter.getUserId(), userId)) {
            throw new IllegalArgumentException("user cannot bind to self");
        }

        DistributionRelation inviterRelation = relationRepository.findByUserId(inviter.getUserId())
                .orElseThrow(() -> new IllegalStateException("inviter relation not found"));

        boolean crossCountry = !inviter.getCountryCode().equalsIgnoreCase(profile.getCountryCode());
        DistributionRelation relation = DistributionRelation.createBound(
                userId,
                profile.getCountryCode(),
                BindSource.INVITE_CODE,
                inviter.getUserId(),
                inviterRelation.getLevel1InviterId(),
                inviterRelation.getLevel2InviterId(),
                crossCountry
        );

        relationRepository.findByUserId(userId).ifPresent(existing -> relationRepository.delete(existing));
        return relationRepository.save(relation);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = randomCode();
        } while (userProfileRepository.existsByInviteCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = random.nextInt(INVITE_CODE_CHARS.length());
            builder.append(INVITE_CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }

    private String normalizeCountry(String countryCode) {
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
