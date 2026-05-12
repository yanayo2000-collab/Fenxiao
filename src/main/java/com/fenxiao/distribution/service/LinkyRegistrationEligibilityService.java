package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.LinkyAccountBinding;
import com.fenxiao.distribution.repository.LinkyAccountBindingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class LinkyRegistrationEligibilityService {

    private final LinkyAccountBindingRepository linkyAccountBindingRepository;
    private final LinkyGuildProbeClient linkyGuildProbeClient;

    public LinkyRegistrationEligibilityService(LinkyAccountBindingRepository linkyAccountBindingRepository,
                                               LinkyGuildProbeClient linkyGuildProbeClient) {
        this.linkyAccountBindingRepository = linkyAccountBindingRepository;
        this.linkyGuildProbeClient = linkyGuildProbeClient;
    }

    public LinkyAccountBinding markEligible(String linkyAccount, String guildId, String guildName, Long checkedBy, String remark) {
        LinkyAccountBinding binding = findOrCreate(linkyAccount);
        binding.markEligible(guildId, guildName, checkedBy, remark);
        return linkyAccountBindingRepository.save(binding);
    }

    public LinkyAccountBinding markJoinedOtherGuild(String linkyAccount, String guildId, String guildName, Long checkedBy, String remark) {
        LinkyAccountBinding binding = findOrCreate(linkyAccount);
        binding.markJoinedOtherGuild(guildId, guildName, checkedBy, remark);
        return linkyAccountBindingRepository.save(binding);
    }

    public LinkyAccountBinding markNotJoined(String linkyAccount, Long checkedBy, String remark) {
        LinkyAccountBinding binding = findOrCreate(linkyAccount);
        binding.markNotJoined(checkedBy, remark);
        return linkyAccountBindingRepository.save(binding);
    }

    public LinkyAccountBinding refreshEligibilityFromProbe(String linkyAccount) {
        LinkyGuildProbeResult result = linkyGuildProbeClient.probe(linkyAccount);
        if (!result.available()) {
            throw new IllegalStateException(result.remark() == null || result.remark().isBlank()
                    ? "guild probe unavailable"
                    : result.remark());
        }
        if (result.matchedOurs()) {
            return markEligible(linkyAccount, result.guildId(), result.guildName(), 0L, result.remark());
        }
        if (result.joinedOtherGuild()) {
            return markJoinedOtherGuild(linkyAccount, result.guildId(), result.guildName(), 0L, result.remark());
        }
        return markNotJoined(linkyAccount, 0L, result.remark());
    }

    public LinkyAccountBinding assertEligibleForRegistration(String linkyAccount) {
        LinkyAccountBinding binding = linkyAccountBindingRepository.findByLinkyAccount(linkyAccount)
                .orElseGet(() -> refreshEligibilityFromProbe(linkyAccount));
        if (!"ELIGIBLE".equals(binding.getRegistrationEligibility())) {
            throw new IllegalStateException("linky account is not eligible for registration");
        }
        return binding;
    }

    public LinkyAccountBinding assertEligibleForExpectedGuild(String linkyAccount, String expectedGuildId, String expectedGuildName, String expectedGuildInviteCode) {
        LinkyAccountBinding binding = linkyAccountBindingRepository.findByLinkyAccount(linkyAccount)
                .orElseGet(() -> refreshEligibilityFromProbe(linkyAccount));
        binding.setExpectedGuild(expectedGuildId, expectedGuildName, expectedGuildInviteCode);
        linkyAccountBindingRepository.save(binding);
        if (!"ELIGIBLE".equals(binding.getRegistrationEligibility()) || binding.getGuildId() == null) {
            throw new IllegalStateException("Please join expected Linky guild with invite code " + expectedGuildInviteCode + " before binding.");
        }
        if (!expectedGuildId.equals(binding.getGuildId())) {
            throw new IllegalStateException("Linky account joined another guild. Please switch to " + expectedGuildName + " using invite code " + expectedGuildInviteCode + ".");
        }
        return binding;
    }

    public BatchRefreshResult refreshAllEligibility() {
        long success = 0;
        long failure = 0;
        for (LinkyAccountBinding binding : linkyAccountBindingRepository.findAll()) {
            try {
                refreshEligibilityFromProbe(binding.getLinkyAccount());
                success++;
            } catch (RuntimeException ex) {
                failure++;
            }
        }
        return new BatchRefreshResult(success, failure);
    }

    public LinkyAccountBinding attachRegisteredUser(String linkyAccount, Long userId, String phoneNumber, String inviteCode) {
        LinkyAccountBinding binding = assertEligibleForRegistration(linkyAccount);
        binding.attachRegistration(userId, phoneNumber, inviteCode);
        return linkyAccountBindingRepository.save(binding);
    }

    public record BatchRefreshResult(long successCount, long failureCount) {}

    private LinkyAccountBinding findOrCreate(String linkyAccount) {
        return linkyAccountBindingRepository.findByLinkyAccount(linkyAccount)
                .orElseGet(() -> LinkyAccountBinding.createUnchecked(linkyAccount));
    }
}
