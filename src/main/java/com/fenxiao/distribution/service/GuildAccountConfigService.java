package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.GuildConfigRequest;
import com.fenxiao.distribution.entity.GuildAccountConfig;
import com.fenxiao.distribution.repository.GuildAccountConfigRepository;
import jakarta.transaction.Transactional;
import com.fenxiao.distribution.api.dto.GuildWeeklyReportResponse;
import com.fenxiao.distribution.repository.LinkyAccountBindingRepository;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.repository.RewardRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class GuildAccountConfigService {
    private final GuildAccountConfigRepository repository;
    private final LinkyAccountBindingRepository linkyAccountBindingRepository;
    private final IncomeEventRepository incomeEventRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final Clock clock;

    public GuildAccountConfigService(GuildAccountConfigRepository repository,
                                     LinkyAccountBindingRepository linkyAccountBindingRepository,
                                     IncomeEventRepository incomeEventRepository,
                                     RewardRecordRepository rewardRecordRepository) {
        this.repository = repository;
        this.linkyAccountBindingRepository = linkyAccountBindingRepository;
        this.incomeEventRepository = incomeEventRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.clock = Clock.systemUTC();
    }

    public GuildAccountConfig expectedGuild(String productCode, Long inviterUserId) {
        String product = normalize(productCode);
        return repository.findByProductCodeAndInviterUserIdAndEnabledTrue(product, inviterUserId)
                .or(() -> repository.findByProductCodeAndInviterUserIdIsNullAndEnabledTrue(product))
                .orElseGet(() -> repository.save(GuildAccountConfig.create(product, null, product + "_DEFAULT_GUILD", product + " Official Guild", "JOIN-" + product)));
    }
    public GuildAccountConfig upsert(GuildConfigRequest request) {
        String product = normalize(request.productCode());
        GuildAccountConfig config = repository.findByProductCodeAndInviterUserIdAndEnabledTrue(product, request.inviterUserId())
                .orElseGet(() -> GuildAccountConfig.create(product, request.inviterUserId(), required(request.guildId(), "guildId"), required(request.guildName(), "guildName"), required(request.guildInviteCode(), "guildInviteCode")));
        config.update(required(request.guildId(), "guildId"), required(request.guildName(), "guildName"), required(request.guildInviteCode(), "guildInviteCode"), request.enabled() == null || request.enabled());
        return repository.save(config);
    }
    public List<GuildAccountConfig> list(String productCode) { return repository.findByProductCodeOrderByIdDesc(normalize(productCode)); }

    public GuildWeeklyReportResponse weeklyReport(String productCode, String guildId, String week) {
        String product = normalize(productCode);
        String normalizedGuildId = required(guildId, "guildId");
        String normalizedWeek = normalizeWeek(week);
        WeekWindow window = resolveWeekWindow(normalizedWeek);
        List<Long> userIds = linkyAccountBindingRepository.findRegisteredUserIdsByGuildId(normalizedGuildId).stream()
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return new GuildWeeklyReportResponse(product, normalizedGuildId, normalizedWeek, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal incomeAmount = incomeEventRepository.sumIncomeAmountByUserIdInAndEventTimeBetween(userIds, window.startAt(), window.endAt());
        BigDecimal rewardAmount = rewardRecordRepository.sumRewardAmountBySourceUserIdInAndCalculatedAtBetween(userIds, window.startAt(), window.endAt());
        return new GuildWeeklyReportResponse(product, normalizedGuildId, normalizedWeek, userIds.size(), incomeAmount, rewardAmount);
    }

    private String normalize(String productCode) { return (productCode == null || productCode.isBlank() ? "LINKY" : productCode.trim().toUpperCase(Locale.ROOT)); }
    private String required(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value.trim(); }

    private String normalizeWeek(String week) { return week == null || week.isBlank() ? "CURRENT" : week.trim().toUpperCase(Locale.ROOT); }

    private WeekWindow resolveWeekWindow(String week) {
        LocalDate baseDate = LocalDate.now(clock);
        if ("PREVIOUS".equals(week) || "LAST".equals(week)) {
            baseDate = baseDate.minusWeeks(1);
        }
        WeekFields weekFields = WeekFields.ISO;
        LocalDate start = baseDate.with(weekFields.dayOfWeek(), 1);
        return new WeekWindow(start.atStartOfDay(), start.plusWeeks(1).atStartOfDay());
    }

    private record WeekWindow(LocalDateTime startAt, LocalDateTime endAt) {}
}
