package com.fenxiao.reward.service;

import com.fenxiao.admin.service.AdminProductScopeService;
import com.fenxiao.distribution.domain.UserStatus;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.income.entity.IncomeEvent;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.api.dto.RewardListItem;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.config.RewardEngineProperties;
import com.fenxiao.reward.domain.IncomeProcessStatus;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.domain.RewardType;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.risk.entity.RiskEvent;
import com.fenxiao.risk.repository.RiskEventRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import com.fenxiao.identity.domain.AccountStatus;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RewardCalculationService {

    private final IncomeEventRepository incomeEventRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final RewardRuleRepository rewardRuleRepository;
    private final DistributionRelationRepository relationRepository;
    private final UserDistributionProfileRepository userProfileRepository;
    private final RiskEventRepository riskEventRepository;
    private final AdminProductScopeService adminProductScopeService;
    private final RewardEngineProperties rewardEngineProperties;

    public RewardCalculationService(IncomeEventRepository incomeEventRepository,
                                    RewardRecordRepository rewardRecordRepository,
                                    RewardRuleRepository rewardRuleRepository,
                                    DistributionRelationRepository relationRepository,
                                    UserDistributionProfileRepository userProfileRepository,
                                    RiskEventRepository riskEventRepository,
                                    AdminProductScopeService adminProductScopeService,
                                    RewardEngineProperties rewardEngineProperties) {
        this.incomeEventRepository = incomeEventRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.rewardRuleRepository = rewardRuleRepository;
        this.relationRepository = relationRepository;
        this.userProfileRepository = userProfileRepository;
        this.riskEventRepository = riskEventRepository;
        this.adminProductScopeService = adminProductScopeService;
        this.rewardEngineProperties = rewardEngineProperties;
    }

    public IncomeProcessStatus processIncomeEvent(String sourceEventId,
                                                  Long userId,
                                                  BigDecimal incomeAmount,
                                                  String currencyCode,
                                                  LocalDateTime eventTime) {
        Optional<IncomeEvent> existingEvent = incomeEventRepository.findBySourceEventId(sourceEventId);
        if (existingEvent.isPresent()) {
            return IncomeProcessStatus.DUPLICATE;
        }
        rewardEngineProperties.assertProcessingEnabled();

        UserDistributionProfile sourceUser = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("source user not found"));
        if (sourceUser.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("source user account is not active");
        }
        DistributionRelation relation = relationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));

        int legacyCandidateCount = countLegacyCandidates(relation);
        RewardRule directRewardRule = preflightDirectRewardRule(sourceUser, relation, eventTime);
        IncomeEvent incomeEvent;

        try {
            incomeEvent = incomeEventRepository.saveAndFlush(IncomeEvent.create(
                    sourceEventId,
                    userId,
                    sourceUser.getCountryCode(),
                    incomeAmount,
                    currencyCode,
                    eventTime,
                    rewardEngineProperties.getVersion().name(),
                    legacyCandidateCount,
                    buildRewardDecision(relation)
            ));
        } catch (DataIntegrityViolationException exception) {
            return IncomeProcessStatus.DUPLICATE;
        }

        sourceUser.addConfirmedIncome(incomeAmount.setScale(6, RoundingMode.HALF_UP));
        userProfileRepository.save(sourceUser);
        relation.lock();
        relationRepository.save(relation);

        boolean riskUser = sourceUser.getUserStatus() == UserStatus.RISK;
        if (riskUser) {
            riskEventRepository.save(RiskEvent.create(userId, "USER_STATUS_RISK", 2, "source user marked as risk"));
        }

        int generatedRewardCount = 0;
        Long beneficiaryId = relation.getLevel1InviterId();
        if (beneficiaryId != null) {
            int rewardLevel = 1;
            rewardRecordRepository.findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(sourceEventId, beneficiaryId, rewardLevel)
                    .orElseGet(() -> saveRewardSafely(buildRewardRecord(
                            sourceEventId,
                            sourceUser,
                            beneficiaryId,
                            incomeAmount,
                            currencyCode,
                            rewardLevel,
                            eventTime,
                            riskUser,
                            directRewardRule
                    )));
            generatedRewardCount = 1;
        }
        incomeEvent.markRewardProcessingCompleted(generatedRewardCount);
        incomeEventRepository.save(incomeEvent);
        return IncomeProcessStatus.PROCESSED;
    }

    private int countLegacyCandidates(DistributionRelation relation) {
        int count = 0;
        if (relation.getLevel1InviterId() != null) count++;
        if (relation.getLevel2InviterId() != null) count++;
        if (relation.getLevel3InviterId() != null) count++;
        return count;
    }

    private String buildRewardDecision(DistributionRelation relation) {
        return "{\"mode\":\"DIRECT_ONLY\",\"legacyCandidateLevels\":[%s],\"blockedLevels\":[%s]}".formatted(
                presentLevels(relation, false),
                presentLevels(relation, true)
        );
    }

    private String presentLevels(DistributionRelation relation, boolean blockedOnly) {
        List<Integer> levels = new ArrayList<>();
        if (!blockedOnly && relation.getLevel1InviterId() != null) levels.add(1);
        if (relation.getLevel2InviterId() != null) levels.add(2);
        if (relation.getLevel3InviterId() != null) levels.add(3);
        return levels.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private RewardRule preflightDirectRewardRule(UserDistributionProfile sourceUser,
                                                  DistributionRelation relation,
                                                  LocalDateTime eventTime) {
        if (relation.getLevel1InviterId() == null) {
            return null;
        }
        List<RewardRule> rules = rewardRuleRepository.findAllEffectiveRules(
                sourceUser.getCountryCode(),
                sourceUser.getDistributionRole().name(),
                1,
                "ACTIVE",
                eventTime
        );
        if (rules.size() != 1) {
            throw new IllegalStateException("exactly one direct reward rule is required");
        }
        RewardRule rule = rules.get(0);
        if (rule.getRewardRate() == null || rule.getRewardRate().signum() <= 0) {
            throw new IllegalStateException("direct reward rate must be positive");
        }
        if (rule.getFreezeDays() == null || rule.getFreezeDays() < 0) {
            throw new IllegalStateException("direct reward freeze days must be non-negative");
        }
        return rule;
    }

    public RewardListResponse getRecentRewards(Long beneficiaryUserId,
                                               RewardStatus status,
                                               LocalDateTime startAt,
                                               LocalDateTime endAt,
                                               int page,
                                               int size,
                                               String productCode) {
        validatePageRequest(page, size);
        String normalizedProductCode = adminProductScopeService.normalizeProductCode(productCode);
        Page<RewardRecord> rewardPage;
        if (normalizedProductCode == null) {
            rewardPage = rewardRecordRepository.findAdminRewards(
                    beneficiaryUserId,
                    status,
                    startAt,
                    endAt,
                    PageRequest.of(page, size)
            );
        } else {
            List<Long> scopedUserIds = adminProductScopeService.resolveScopedUserIds(normalizedProductCode);
            if (scopedUserIds.isEmpty()) {
                return new RewardListResponse(List.of(), 0, page, size);
            }
            if (beneficiaryUserId != null) {
                if (!scopedUserIds.contains(beneficiaryUserId)) {
                    return new RewardListResponse(List.of(), 0, page, size);
                }
                scopedUserIds = List.of(beneficiaryUserId);
            }
            rewardPage = rewardRecordRepository.findAdminRewardsByBeneficiaryUserIdIn(
                    scopedUserIds,
                    status,
                    startAt,
                    endAt,
                    PageRequest.of(page, size)
            );
        }

        List<RewardListItem> items = new ArrayList<>();
        for (RewardRecord record : rewardPage.getContent()) {
            items.add(new RewardListItem(
                    record.getBeneficiaryUserId(),
                    record.getSourceUserId(),
                    record.getRewardLevel(),
                    record.getRewardAmount(),
                    record.getRewardStatus(),
                    record.getCalculatedAt()
            ));
        }
        return new RewardListResponse(items, rewardPage.getTotalElements(), page, size);
    }

    public int unlockDueRewards(LocalDateTime now) {
        rewardEngineProperties.assertProcessingEnabled();
        List<RewardRecord> dueRecords = rewardRecordRepository
                .findByRewardStatusAndUnfreezeAtLessThanEqualAndRewardTypeAndRewardLevel(
                        RewardStatus.FROZEN,
                        now,
                        RewardType.DIRECT_RECRUIT,
                        1
                );
        dueRecords.forEach(RewardRecord::markAvailable);
        rewardRecordRepository.saveAll(dueRecords);
        return dueRecords.size();
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private RewardRecord buildRewardRecord(String sourceEventId,
                                           UserDistributionProfile sourceUser,
                                           Long beneficiaryId,
                                           BigDecimal incomeAmount,
                                           String currencyCode,
                                           int rewardLevel,
                                           LocalDateTime eventTime,
                                           boolean riskUser,
                                           RewardRule rule) {

        BigDecimal rewardAmount = incomeAmount.multiply(rule.getRewardRate()).setScale(6, RoundingMode.HALF_UP);
        RewardRecord record = RewardRecord.create(
                sourceEventId,
                beneficiaryId,
                sourceUser.getUserId(),
                rewardLevel,
                incomeAmount.setScale(6, RoundingMode.HALF_UP),
                rule.getRewardRate(),
                rewardAmount,
                currencyCode,
                rule.getFreezeDays(),
                eventTime,
                rewardEngineProperties.getVersion().name(),
                RewardType.DIRECT_RECRUIT
        );
        if (riskUser) {
            record.markRiskHold("source user marked as risk");
        }
        return record;
    }

    private RewardRecord saveRewardSafely(RewardRecord record) {
        try {
            return rewardRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            return rewardRecordRepository.findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(
                    record.getSourceEventId(),
                    record.getBeneficiaryUserId(),
                    record.getRewardLevel()
            ).orElseThrow(() -> exception);
        }
    }
}
