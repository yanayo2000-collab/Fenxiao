package com.fenxiao.incentive.service;

import com.fenxiao.incentive.dto.*;
import com.fenxiao.platform.entity.PlatformAccountBinding;
import com.fenxiao.platform.entity.PlatformLifecycleSnapshot;
import com.fenxiao.platform.repository.PlatformAccountBindingRepository;
import com.fenxiao.platform.repository.PlatformLifecycleSnapshotRepository;
import com.fenxiao.relationship.repository.InvitationRelationVersionRepository;
import com.fenxiao.relationship.repository.MentorAssignmentVersionRepository;
import com.fenxiao.relationship.repository.OperatingTeamRepository;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.sql.PreparedStatement;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class IncentiveShadowService {
    private final JdbcTemplate jdbc;
    private final PlatformLifecycleSnapshotRepository snapshotRepository;
    private final PlatformAccountBindingRepository bindingRepository;
    private final InvitationRelationVersionRepository invitationRepository;
    private final MentorAssignmentVersionRepository mentorRepository;
    private final OperatingTeamRepository teamRepository;
    private final UserDistributionProfileRepository userRepository;
    private final Clock clock;

    public IncentiveShadowService(JdbcTemplate jdbc, PlatformLifecycleSnapshotRepository snapshotRepository,
                                  PlatformAccountBindingRepository bindingRepository,
                                  InvitationRelationVersionRepository invitationRepository,
                                  MentorAssignmentVersionRepository mentorRepository,
                                  OperatingTeamRepository teamRepository,
                                  UserDistributionProfileRepository userRepository, Clock clock) {
        this.jdbc = jdbc; this.snapshotRepository = snapshotRepository; this.bindingRepository = bindingRepository;
        this.invitationRepository = invitationRepository; this.mentorRepository = mentorRepository;
        this.teamRepository = teamRepository; this.userRepository = userRepository; this.clock = clock;
    }

    public long configureMentorRule(MentorRewardRuleRequest request) {
        LocalDateTime at = request.effectiveFrom() == null ? LocalDateTime.now(clock) : request.effectiveFrom();
        String code = upper(request.ruleCode());
        Integer version = jdbc.queryForObject("select coalesce(max(rule_version),0)+1 from incentive_rule_version where rule_code=?", Integer.class, code);
        jdbc.update("update incentive_rule_version set enabled=false,effective_to=? where rule_code=? and enabled=true", at, code);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("insert into incentive_rule_version(rule_code,rule_version,reward_type,milestone_code,platform_code,country_code,guild_id,amount_minor,currency_code,freeze_days,effective_from,enabled) values(?,?,'MENTOR',?,?,?,?,?,?,?,?,true)", new String[]{"id"});
            statement.setString(1, code); statement.setInt(2, version); statement.setString(3, upper(request.milestoneCode()));
            statement.setString(4, upper(request.platformCode())); statement.setString(5, upper(request.countryCode())); statement.setString(6, trim(request.guildId()));
            statement.setLong(7, request.amountMinor()); statement.setString(8, upper(request.currencyCode())); statement.setInt(9, request.freezeDays()); statement.setObject(10, at);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public long configureLeadershipPolicy(LeadershipPolicyRequest request) {
        if (request.profitShareRate() == null) throw new IllegalArgumentException("profit share rate is required");
        LocalDateTime at = request.effectiveFrom() == null ? LocalDateTime.now(clock) : request.effectiveFrom();
        String code = upper(request.policyCode());
        Integer version = jdbc.queryForObject("select coalesce(max(policy_version),0)+1 from leadership_policy_version where policy_code=?", Integer.class, code);
        jdbc.update("update leadership_policy_version set enabled=false,effective_to=? where policy_code=? and enabled=true", at, code);
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("insert into leadership_policy_version(policy_code,policy_version,platform_code,country_code,guild_id,required_valid_starts,required_withdraw_eligible,required_active_7d,profit_share_rate,effective_from,enabled) values(?,?,?,?,?,?,?,?,?,?,true)", new String[]{"id"});
            statement.setString(1, code); statement.setInt(2, version); statement.setString(3, upper(request.platformCode())); statement.setString(4, upper(request.countryCode())); statement.setString(5, trim(request.guildId()));
            statement.setInt(6, request.requiredValidStarts()); statement.setInt(7, request.requiredWithdrawEligible()); statement.setInt(8, request.requiredActive7d()); statement.setBigDecimal(9, request.profitShareRate()); statement.setObject(10, at);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public void evaluateLifecycle(PlatformLifecycleSnapshot snapshot) {
        mentorRepository.findTopByStudentUserIdOrderByVersionNoDesc(snapshot.getUserId())
                .filter(value -> value.getMentorUserId() != null && value.getEffectiveTo() == null)
                .ifPresent(value -> createMentorEntries(value.getMentorUserId(), snapshot));
        invitationRepository.findTopByUserIdOrderByVersionNoDesc(snapshot.getUserId())
                .map(value -> value.getInviterUserId())
                .filter(Objects::nonNull)
                .ifPresent(inviter -> evaluateLeadership(inviter, snapshot.getPlatformCode()));
    }

    public QualificationResult evaluateLeadership(Long userId, String platformCode) {
        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        String platform = upper(platformCode);
        String guildId = bindingRepository.findByUserIdAndPlatformCode(userId, platform)
                .map(PlatformAccountBinding::getOfficialGuildId)
                .filter(value -> !value.isBlank())
                .orElse(null);
        List<Long> directIds = invitationRepository.findByInviterUserIdAndEffectiveToIsNull(userId).stream().map(value -> value.getUserId()).toList();
        Set<Long> scopedIds = directIds.isEmpty() ? Set.of() : bindingRepository.findByUserIdInAndPlatformCode(directIds, platform).stream()
                .filter(value -> Objects.equals(guildId, value.getOfficialGuildId()))
                .map(PlatformAccountBinding::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        List<PlatformLifecycleSnapshot> snapshots = scopedIds.isEmpty() ? List.of() : snapshotRepository.findByUserIdInAndPlatformCode(scopedIds, platform);
        int valid = (int) snapshots.stream().filter(PlatformLifecycleSnapshot::isValid72HourStart).count();
        int withdraw = (int) snapshots.stream().filter(value -> value.getFirstWithdrawEligibleAt() != null).count();
        int active7 = (int) snapshots.stream().filter(PlatformLifecycleSnapshot::isConsecutive7DayActive).count();
        List<LeadershipPolicy> policies = jdbc.query("select id,required_valid_starts,required_withdraw_eligible,required_active_7d,profit_share_rate from leadership_policy_version where platform_code=? and country_code=? and enabled=true and effective_from<=? and effective_to is null and ((? is null and guild_id is null) or guild_id=?) order by policy_version desc limit 1",
                (rs, row) -> new LeadershipPolicy(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getBigDecimal(5)), platform, user.getCountryCode(), LocalDateTime.now(clock), guildId, guildId);
        if (policies.isEmpty()) return new QualificationResult(valid, withdraw, active7, false, false, null);
        LeadershipPolicy policy = policies.get(0);
        boolean newStar = valid >= policy.validStarts();
        boolean share5 = newStar && withdraw >= policy.withdrawEligible() && active7 >= policy.active7();
        upsertQualification(userId, platform, guildId, "NEW_STAR_TEAM_LEADER", newStar, policy.id(), valid, withdraw, active7);
        upsertQualification(userId, platform, guildId, "TEAM_PROFIT_SHARE", share5, policy.id(), valid, withdraw, active7);
        return new QualificationResult(valid, withdraw, active7, newStar, share5, policy.shareRate());
    }

    public TeamProfitResult ingestTeamProfit(TeamProfitFactRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) throw new IllegalArgumentException("profit period is invalid");
        requireNonNegative(request.businessIncomeMinor(), "business income");
        requireNonNegative(request.directCostMinor(), "direct cost");
        requireNonNegative(request.recruiterRewardMinor(), "recruiter reward");
        requireNonNegative(request.mentorRewardMinor(), "mentor reward");
        requireNonNegative(request.paymentAdjustmentMinor(), "payment adjustment");
        int existing = jdbc.queryForObject("select count(*) from team_profit_fact where source_system=? and source_event_id=?", Integer.class, request.sourceSystem(), request.sourceEventId());
        if (existing > 0) return new TeamProfitResult(0, 0, false);
        long profit = request.businessIncomeMinor() - request.directCostMinor() - request.recruiterRewardMinor() - request.mentorRewardMinor() - request.paymentAdjustmentMinor();
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("insert into team_profit_fact(source_event_id,team_id,platform_code,period_start,period_end,business_income_minor,direct_cost_minor,recruiter_reward_minor,mentor_reward_minor,payment_adjustment_minor,operating_profit_minor,currency_code,source_system,received_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", new String[]{"id"});
            statement.setString(1, request.sourceEventId()); statement.setLong(2, request.teamId()); statement.setString(3, upper(request.platformCode())); statement.setObject(4, request.periodStart()); statement.setObject(5, request.periodEnd());
            statement.setLong(6, request.businessIncomeMinor()); statement.setLong(7, request.directCostMinor()); statement.setLong(8, request.recruiterRewardMinor()); statement.setLong(9, request.mentorRewardMinor()); statement.setLong(10, request.paymentAdjustmentMinor()); statement.setLong(11, profit); statement.setString(12, upper(request.currencyCode())); statement.setString(13, request.sourceSystem()); statement.setObject(14, LocalDateTime.now(clock)); return statement;
        }, keys);
        long factId = Objects.requireNonNull(keys.getKey()).longValue();
        var team = teamRepository.findById(request.teamId()).orElseThrow(() -> new IllegalArgumentException("team not found"));
        if (team.getLeaderUserId() == null || profit <= 0) return new TeamProfitResult(profit, 0, false);
        QualificationResult qualification = evaluateLeadership(team.getLeaderUserId(), request.platformCode());
        if (!qualification.profitShareQualified() || qualification.shareRate() == null) return new TeamProfitResult(profit, 0, false);
        long share = BigDecimal.valueOf(profit).multiply(qualification.shareRate()).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String guildId = bindingRepository.findByUserIdAndPlatformCode(team.getLeaderUserId(), upper(request.platformCode()))
                .map(PlatformAccountBinding::getOfficialGuildId).orElse(null);
        jdbc.update("insert into team_profit_share_shadow_ledger(idempotency_key,team_profit_fact_id,team_id,leader_user_id,platform_code,policy_id,share_rate,share_amount_minor,currency_code,ledger_status,triggered_at) select ?,?,?,?,?,q.policy_id,?,?,?,'SHADOW',? from leadership_qualification q where q.user_id=? and q.platform_code=? and q.guild_id=? and q.qualification_code='TEAM_PROFIT_SHARE' and q.qualification_status='QUALIFIED'",
                "TEAM_SHARE:" + request.sourceSystem() + ":" + request.sourceEventId(), factId, request.teamId(), team.getLeaderUserId(), upper(request.platformCode()), qualification.shareRate(), share, upper(request.currencyCode()), LocalDateTime.now(clock), team.getLeaderUserId(), upper(request.platformCode()), guildId);
        return new TeamProfitResult(profit, share, true);
    }

    private void createMentorEntries(Long mentorId, PlatformLifecycleSnapshot snapshot) {
        List<String> milestones = new ArrayList<>();
        if (snapshot.isValid72HourStart()) milestones.add("VALID_72H_START");
        if (snapshot.getFirstIncomeAt() != null) milestones.add("FIRST_INCOME");
        if (snapshot.getFirstWithdrawEligibleAt() != null) milestones.add("FIRST_WITHDRAW_ELIGIBLE");
        if (snapshot.isConsecutive7DayActive()) milestones.add("ACTIVE_7D");
        if (snapshot.isConsecutive30DayActive()) milestones.add("ACTIVE_30D");
        PlatformAccountBinding binding = bindingRepository.findByUserIdAndPlatformCode(snapshot.getUserId(), snapshot.getPlatformCode()).orElseThrow();
        String country = userRepository.findById(snapshot.getUserId()).orElseThrow().getCountryCode();
        for (String milestone : milestones) {
            List<Rule> rules = jdbc.query("select id,rule_version,amount_minor,currency_code from incentive_rule_version where reward_type='MENTOR' and milestone_code=? and platform_code=? and country_code=? and enabled=true and effective_from<=? and effective_to is null and (guild_id is null or guild_id=?) order by rule_version desc limit 1",
                    (rs, row) -> new Rule(rs.getLong(1), rs.getInt(2), rs.getLong(3), rs.getString(4)), milestone, snapshot.getPlatformCode(), country, LocalDateTime.now(clock), binding.getOfficialGuildId());
            if (rules.isEmpty()) continue;
            Rule rule = rules.get(0);
            String key = "MENTOR:" + mentorId + ":" + snapshot.getUserId() + ":" + snapshot.getPlatformCode() + ":" + milestone + ":" + rule.id();
            if (jdbc.queryForObject("select count(*) from incentive_shadow_ledger where idempotency_key=?", Integer.class, key) == 0) {
                jdbc.update("insert into incentive_shadow_ledger(idempotency_key,recipient_user_id,source_user_id,platform_code,platform_user_id,reward_type,milestone_code,rule_id,rule_version,amount_minor,currency_code,ledger_status,source_snapshot_id,triggered_at) values(?,?,?,?,?,'MENTOR',?,?,?,?,?,'SHADOW',?,?)",
                        key, mentorId, snapshot.getUserId(), snapshot.getPlatformCode(), snapshot.getPlatformUserId(), milestone, rule.id(), rule.version(), rule.amountMinor(), rule.currency(), snapshot.getId(), LocalDateTime.now(clock));
            }
        }
    }

    private void upsertQualification(Long userId, String platform, String guildId, String code, boolean qualified, long policyId, int valid, int withdraw, int active7) {
        String status = qualified ? "QUALIFIED" : "IN_PROGRESS";
        LocalDateTime now = LocalDateTime.now(clock);
        if (guildId == null) return;
        int updated = jdbc.update("update leadership_qualification set qualification_status=?,policy_id=?,valid_start_count=?,withdraw_eligible_count=?,active_7d_count=?,qualified_at=case when ?='QUALIFIED' and qualified_at is null then ? else qualified_at end,evaluated_at=?,shadow_only=true where user_id=? and platform_code=? and guild_id=? and qualification_code=?",
                status, policyId, valid, withdraw, active7, status, now, now, userId, platform, guildId, code);
        if (updated == 0) jdbc.update("insert into leadership_qualification(user_id,platform_code,guild_id,qualification_code,qualification_status,policy_id,valid_start_count,withdraw_eligible_count,active_7d_count,qualified_at,evaluated_at,shadow_only) values(?,?,?,?,?,?,?,?,?,?,?,true)",
                userId, platform, guildId, code, status, policyId, valid, withdraw, active7, qualified ? now : null, now);
    }

    public Map<String, Long> shadowCounts() {
        return Map.of("mentorRewards", jdbc.queryForObject("select count(*) from incentive_shadow_ledger", Long.class),
                "leadershipQualifications", jdbc.queryForObject("select count(*) from leadership_qualification", Long.class),
                "teamProfitShares", jdbc.queryForObject("select count(*) from team_profit_share_shadow_ledger", Long.class));
    }

    private String upper(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("value is required"); return value.trim().toUpperCase(Locale.ROOT); }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void requireNonNegative(long value, String label) { if (value < 0) throw new IllegalArgumentException(label + " must be non-negative"); }
    private record Rule(long id, int version, long amountMinor, String currency) {}
    private record LeadershipPolicy(long id, int validStarts, int withdrawEligible, int active7, BigDecimal shareRate) {}
    public record QualificationResult(int validStartCount, int withdrawEligibleCount, int active7Count, boolean newStarQualified, boolean profitShareQualified, BigDecimal shareRate) {}
    public record TeamProfitResult(long operatingProfitMinor, long shareAmountMinor, boolean shadowLedgerCreated) {}
}
