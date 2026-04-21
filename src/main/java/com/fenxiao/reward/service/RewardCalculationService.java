package com.fenxiao.reward.service;

import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.income.entity.IncomeEvent;
import com.fenxiao.income.repository.IncomeEventRepository;
import com.fenxiao.reward.api.dto.RewardListItem;
import com.fenxiao.reward.api.dto.RewardListResponse;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.rule.entity.RewardRule;
import com.fenxiao.rule.repository.RewardRuleRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
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

    public RewardCalculationService(IncomeEventRepository incomeEventRepository,
                                    RewardRecordRepository rewardRecordRepository,
                                    RewardRuleRepository rewardRuleRepository,
                                    DistributionRelationRepository relationRepository,
                                    UserDistributionProfileRepository userProfileRepository) {
        this.incomeEventRepository = incomeEventRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.rewardRuleRepository = rewardRuleRepository;
        this.relationRepository = relationRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public void processIncomeEvent(String sourceEventId,
                                   Long userId,
                                   BigDecimal incomeAmount,
                                   String currencyCode,
                                   LocalDateTime eventTime) {
        Optional<IncomeEvent> existingEvent = incomeEventRepository.findBySourceEventId(sourceEventId);
        if (existingEvent.isPresent()) {
            return;
        }

        UserDistributionProfile sourceUser = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("source user not found"));
        DistributionRelation relation = relationRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("distribution relation not found"));

        try {
            incomeEventRepository.saveAndFlush(IncomeEvent.create(
                    sourceEventId,
                    userId,
                    sourceUser.getCountryCode(),
                    incomeAmount,
                    currencyCode,
                    eventTime
            ));
        } catch (DataIntegrityViolationException exception) {
            return;
        }

        List<Long> beneficiaries = new ArrayList<>();
        beneficiaries.add(relation.getLevel1InviterId());
        beneficiaries.add(relation.getLevel2InviterId());
        beneficiaries.add(relation.getLevel3InviterId());

        for (int i = 0; i < beneficiaries.size(); i++) {
            Long beneficiaryId = beneficiaries.get(i);
            if (beneficiaryId == null) {
                continue;
            }
            int rewardLevel = i + 1;
            rewardRecordRepository.findBySourceEventIdAndBeneficiaryUserIdAndRewardLevel(sourceEventId, beneficiaryId, rewardLevel)
                    .orElseGet(() -> saveRewardSafely(buildRewardRecord(
                            sourceEventId,
                            sourceUser,
                            beneficiaryId,
                            incomeAmount,
                            currencyCode,
                            rewardLevel,
                            eventTime
                    )));
        }
    }

    public RewardListResponse getRecentRewards() {
        List<RewardListItem> items = new ArrayList<>();
        List<RewardRecord> records = rewardRecordRepository.findTop50ByOrderByIdDesc();
        for (RewardRecord record : records) {
            items.add(new RewardListItem(
                    record.getBeneficiaryUserId(),
                    record.getSourceUserId(),
                    record.getRewardLevel(),
                    record.getRewardAmount(),
                    record.getRewardStatus(),
                    record.getCalculatedAt()
            ));
        }
        return new RewardListResponse(items, records.size());
    }

    private RewardRecord buildRewardRecord(String sourceEventId,
                                           UserDistributionProfile sourceUser,
                                           Long beneficiaryId,
                                           BigDecimal incomeAmount,
                                           String currencyCode,
                                           int rewardLevel,
                                           LocalDateTime eventTime) {
        RewardRule rule = rewardRuleRepository.findEffectiveRule(
                        sourceUser.getCountryCode(),
                        sourceUser.getDistributionRole().name(),
                        rewardLevel,
                        "ACTIVE",
                        eventTime
                )
                .orElseThrow(() -> new IllegalStateException("reward rule not found"));

        BigDecimal rewardAmount = incomeAmount.multiply(rule.getRewardRate()).setScale(6, RoundingMode.HALF_UP);
        return RewardRecord.create(
                sourceEventId,
                beneficiaryId,
                sourceUser.getUserId(),
                rewardLevel,
                incomeAmount.setScale(6, RoundingMode.HALF_UP),
                rule.getRewardRate(),
                rewardAmount,
                currencyCode,
                rule.getFreezeDays()
        );
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
