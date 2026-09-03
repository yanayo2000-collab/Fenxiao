package com.fenxiao.platform.service;

import com.fenxiao.platform.domain.PlatformBindingStatus;
import com.fenxiao.platform.domain.PlatformFactType;
import com.fenxiao.platform.dto.PlatformBusinessFactRequest;
import com.fenxiao.platform.dto.VerifyPlatformBindingRequest;
import com.fenxiao.platform.entity.*;
import com.fenxiao.platform.repository.*;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import com.fenxiao.incentive.service.IncentiveShadowService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class PlatformLifecycleService {
    private final PlatformAccountBindingRepository bindingRepository;
    private final PlatformBindingHistoryRepository historyRepository;
    private final PlatformBusinessFactRepository factRepository;
    private final PlatformMilestonePolicyRepository policyRepository;
    private final PlatformLifecycleSnapshotRepository snapshotRepository;
    private final UserDistributionProfileRepository userRepository;
    private final IncentiveShadowService incentiveShadowService;
    private final boolean shadowOnly;
    private final Clock clock;

    public PlatformLifecycleService(PlatformAccountBindingRepository bindingRepository,
                                    PlatformBindingHistoryRepository historyRepository,
                                    PlatformBusinessFactRepository factRepository,
                                    PlatformMilestonePolicyRepository policyRepository,
                                    PlatformLifecycleSnapshotRepository snapshotRepository,
                                    UserDistributionProfileRepository userRepository,
                                    IncentiveShadowService incentiveShadowService,
                                    @Value("${app.distribution.lifecycle.shadow-only:true}") boolean shadowOnly,
                                    Clock clock) {
        this.bindingRepository = bindingRepository; this.historyRepository = historyRepository;
        this.factRepository = factRepository; this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository; this.userRepository = userRepository;
        this.incentiveShadowService = incentiveShadowService;
        this.shadowOnly = shadowOnly; this.clock = clock;
    }

    public PlatformAccountBinding submit(Long userId, String platformCode, String platformUserId) {
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        String platform = normalizePlatform(platformCode);
        String externalId = normalizePlatformUserId(platformUserId);
        bindingRepository.findByUserIdAndPlatformCode(userId, platform).ifPresent(value -> {
            throw new IllegalStateException("user already has a binding for platform " + platform);
        });
        bindingRepository.findByPlatformCodeAndPlatformUserId(platform, externalId).ifPresent(value -> {
            throw new IllegalStateException("platform id has already been recorded");
        });
        LocalDateTime now = LocalDateTime.now(clock);
        PlatformAccountBinding binding = bindingRepository.save(PlatformAccountBinding.submit(userId, platform, externalId, now));
        historyRepository.save(PlatformBindingHistory.record(binding, null, "USER_SUBMITTED", null, "BANDEIRA", userId, now));
        return binding;
    }

    public PlatformAccountBinding verify(VerifyPlatformBindingRequest request) {
        String platform = normalizePlatform(request.platformCode());
        PlatformAccountBinding binding = bindingRepository.findByPlatformCodeAndPlatformUserId(platform, normalizePlatformUserId(request.platformUserId()))
                .orElseThrow(() -> new IllegalArgumentException("platform binding not found"));
        PlatformBindingStatus before = binding.getBindingStatus();
        String rejectionCode = null;
        String rejectionReason = null;
        if (request.globallySeenBeforeSubmission()) {
            rejectionCode = "PREEXISTING_GLOBAL_ID";
            rejectionReason = "platform id existed in an authoritative system before submission";
        } else if (!request.joinedTargetGuild()) {
            rejectionCode = "NOT_IN_TARGET_GUILD";
            rejectionReason = "platform id is not in the target guild";
        } else if (Math.abs(ChronoUnit.DAYS.between(binding.getSubmittedAt().toLocalDate(), request.officialJoinedAt().toLocalDate())) > 1) {
            rejectionCode = "JOIN_DATE_MISMATCH";
            rejectionReason = "official guild join date is outside the submission date tolerance";
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (rejectionCode != null) {
            binding.reject(rejectionCode, rejectionReason, request.sourceSystem());
            historyRepository.save(PlatformBindingHistory.record(binding, before, rejectionCode, rejectionReason, request.sourceSystem(), null, now));
            return binding;
        }
        binding.verify(request.officialGuildId(), request.officialJoinedAt(), request.sourceSystem(), request.sourceReference(), now);
        historyRepository.save(PlatformBindingHistory.record(binding, before, "AUTHORITATIVE_VERIFIED", null, request.sourceSystem(), null, now));
        evaluate(binding);
        return binding;
    }

    public PlatformLifecycleSnapshot ingest(PlatformBusinessFactRequest request) {
        if (factRepository.existsBySourceSystemAndSourceEventId(request.sourceSystem(), request.sourceEventId())) {
            PlatformAccountBinding existing = requireVerified(request.platformCode(), request.platformUserId());
            return evaluate(existing);
        }
        PlatformAccountBinding binding = requireVerified(request.platformCode(), request.platformUserId());
        if (request.factType() == PlatformFactType.NET_INCOME && (request.amount() == null || request.amount().signum() < 0)) {
            throw new IllegalArgumentException("net income amount must be zero or positive");
        }
        factRepository.save(PlatformBusinessFact.create(request.sourceEventId(), binding, request.factType(), request.amount(),
                request.currencyCode(), request.occurredAt(), request.guildId(), request.sourceSystem(), request.sourceVersion(),
                request.payloadHash(), LocalDateTime.now(clock)));
        return evaluate(binding);
    }

    public PlatformLifecycleSnapshot get(Long userId, String platformCode) {
        return snapshotRepository.findByUserIdAndPlatformCode(userId, normalizePlatform(platformCode))
                .orElseThrow(() -> new IllegalArgumentException("platform lifecycle not found"));
    }

    public PlatformAccountBinding getBinding(Long userId, String platformCode) {
        return bindingRepository.findByUserIdAndPlatformCode(userId, normalizePlatform(platformCode))
                .orElseThrow(() -> new IllegalArgumentException("platform binding not found"));
    }

    public PlatformMilestonePolicy configurePolicy(String platformCode, String guildId, String countryCode,
                                                    BigDecimal minimum, String currencyCode, LocalDateTime effectiveFrom) {
        String platform = normalizePlatform(platformCode);
        String guild = requireText(guildId, "guild id");
        String country = requireText(countryCode, "country code").toUpperCase(Locale.ROOT);
        LocalDateTime at = effectiveFrom == null ? LocalDateTime.now(clock) : effectiveFrom;
        policyRepository.findByPlatformCodeAndGuildIdAndCountryCodeAndEnabledTrue(platform, guild, country)
                .forEach(value -> value.closeAt(at));
        return policyRepository.save(PlatformMilestonePolicy.create(platform, guild, country, minimum, requireText(currencyCode, "currency code"), at));
    }

    private PlatformLifecycleSnapshot evaluate(PlatformAccountBinding binding) {
        List<PlatformBusinessFact> facts = factRepository.findByUserIdAndPlatformCodeOrderByOccurredAtAscIdAsc(binding.getUserId(), binding.getPlatformCode());
        List<PlatformBusinessFact> incomes = facts.stream().filter(value -> value.getFactType() == PlatformFactType.NET_INCOME && value.getAmount() != null && value.getAmount().signum() > 0).toList();
        BigDecimal cumulative = incomes.stream().map(PlatformBusinessFact::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime firstIncome = incomes.isEmpty() ? null : incomes.get(0).getOccurredAt();
        var user = userRepository.findById(binding.getUserId()).orElseThrow();
        BigDecimal threshold = policyRepository.findTopByPlatformCodeAndGuildIdAndCountryCodeAndEnabledTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByEffectiveFromDesc(
                        binding.getPlatformCode(), binding.getOfficialGuildId(), user.getCountryCode(), LocalDateTime.now(clock))
                .map(PlatformMilestonePolicy::getMinimumWithdrawableAmount).orElse(null);
        LocalDateTime eligibleAt = null;
        if (threshold != null) {
            BigDecimal running = BigDecimal.ZERO;
            for (PlatformBusinessFact income : incomes) {
                running = running.add(income.getAmount());
                if (running.compareTo(threshold) >= 0) { eligibleAt = income.getOccurredAt(); break; }
            }
        }
        Set<LocalDate> activeDates = new HashSet<>();
        for (PlatformBusinessFact income : incomes) activeDates.add(income.getBusinessDate());
        int consecutive = 0;
        if (binding.getOfficialJoinedAt() != null) {
            LocalDate date = binding.getOfficialJoinedAt().toLocalDate();
            while (activeDates.contains(date)) { consecutive++; date = date.plusDays(1); }
        }
        LocalDateTime now = LocalDateTime.now(clock);
        PlatformLifecycleSnapshot snapshot = snapshotRepository.findByUserIdAndPlatformCode(binding.getUserId(), binding.getPlatformCode())
                .orElseGet(() -> PlatformLifecycleSnapshot.create(binding, shadowOnly, now));
        boolean valid72HourStart = binding.getVerifiedAt() != null && !binding.getVerifiedAt().isAfter(user.getRegisteredAt().plusHours(72));
        snapshot.evaluate(binding, valid72HourStart, firstIncome, eligibleAt, consecutive, cumulative, shadowOnly, now);
        PlatformLifecycleSnapshot saved = snapshotRepository.save(snapshot);
        incentiveShadowService.evaluateLifecycle(saved);
        return saved;
    }

    private PlatformAccountBinding requireVerified(String platformCode, String platformUserId) {
        return bindingRepository.findByPlatformCodeAndPlatformUserId(normalizePlatform(platformCode), normalizePlatformUserId(platformUserId))
                .filter(value -> value.getBindingStatus() == PlatformBindingStatus.VERIFIED)
                .orElseThrow(() -> new IllegalStateException("platform binding is not verified"));
    }
    private String normalizePlatform(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("platform code is required");
        String platform = value.trim().toUpperCase(Locale.ROOT);
        if (!platform.matches("^[A-Z][A-Z0-9_]{1,31}$")) throw new IllegalArgumentException("platform code is invalid");
        return platform;
    }
    private String normalizePlatformUserId(String value) {
        if (value == null || !value.trim().matches("^[0-9]{5,32}$")) throw new IllegalArgumentException("platform user id must be numeric");
        return value.trim();
    }
    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }
}
