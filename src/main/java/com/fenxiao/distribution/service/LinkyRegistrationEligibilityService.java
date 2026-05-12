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

    public LinkyAccountBinding attachRegisteredUser(String linkyAccount, Long userId, String phoneNumber, String inviteCode) {
        LinkyAccountBinding binding = assertEligibleForRegistration(linkyAccount);
        binding.attachRegistration(userId, phoneNumber, inviteCode);
        return linkyAccountBindingRepository.save(binding);
    }

    private LinkyAccountBinding findOrCreate(String linkyAccount) {
        return linkyAccountBindingRepository.findByLinkyAccount(linkyAccount)
                .orElseGet(() -> LinkyAccountBinding.createUnchecked(linkyAccount));
    }
}
