package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class WithdrawRequestServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private RewardRecordRepository rewardRecordRepository;

    @Autowired
    private WithdrawRequestService withdrawRequestService;

    @Test
    void shouldCreateWithdrawRequestFromAvailableRewards() {
        UserDistributionProfile profile = distributionBindingService.createProfile(61001L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-61001-a", profile.getUserId(), 1, "600.000000"));
        rewardRecordRepository.save(makeAvailableReward("evt-61001-b", profile.getUserId(), 2, "450.000000"));

        WithdrawRequest request = withdrawRequestService.createRequest(profile.getUserId());

        assertThat(request.getUserId()).isEqualTo(profile.getUserId());
        assertThat(request.getRequestedDiamondAmount()).isEqualByComparingTo("1050.000000");
        assertThat(request.getRequestStatus()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void shouldRejectSecondWithdrawRequestInSameWeek() {
        UserDistributionProfile profile = distributionBindingService.createProfile(61002L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-61002-a", profile.getUserId(), 1, "1200.000000"));

        withdrawRequestService.createRequest(profile.getUserId());

        assertThatThrownBy(() -> withdrawRequestService.createRequest(profile.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("withdraw request already submitted this week");
    }

    @Test
    void shouldRejectWithdrawRequestBelowMinimumDiamondThreshold() {
        UserDistributionProfile profile = distributionBindingService.createProfile(61003L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-61003-a", profile.getUserId(), 1, "999.000000"));

        assertThatThrownBy(() -> withdrawRequestService.createRequest(profile.getUserId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("minimum withdraw amount is 1000 diamonds");
    }

    @Test
    void shouldApproveWithdrawRequestAndMarkRewardsPaidOut() {
        UserDistributionProfile profile = distributionBindingService.createProfile(61004L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-61004-a", profile.getUserId(), 1, "800.000000"));
        rewardRecordRepository.save(makeAvailableReward("evt-61004-b", profile.getUserId(), 2, "500.000000"));
        WithdrawRequest request = withdrawRequestService.createRequest(profile.getUserId());

        WithdrawRequest approved = withdrawRequestService.approveRequest(request.getRequestNo(), 90001L, "manual payout done");

        assertThat(approved.getRequestStatus()).isEqualTo("PAID_OUT");
        assertThat(rewardRecordRepository.findByBeneficiaryUserIdOrderByIdDesc(profile.getUserId()))
                .extracting(RewardRecord::getWithdrawStatus)
                .containsOnly("PAID_OUT");
    }

    @Test
    void shouldRejectWithdrawRequestAndRestoreRewardsToUnclaimed() {
        UserDistributionProfile profile = distributionBindingService.createProfile(61005L, "ID", "id", null);
        rewardRecordRepository.save(makeAvailableReward("evt-61005-a", profile.getUserId(), 1, "1100.000000"));
        WithdrawRequest request = withdrawRequestService.createRequest(profile.getUserId());

        WithdrawRequest rejected = withdrawRequestService.rejectRequest(request.getRequestNo(), 90002L, "bank account mismatch");

        assertThat(rejected.getRequestStatus()).isEqualTo("REJECTED");
        assertThat(rewardRecordRepository.findByBeneficiaryUserIdOrderByIdDesc(profile.getUserId()))
                .extracting(RewardRecord::getWithdrawStatus)
                .containsOnly("UNCLAIMED");
    }

    private RewardRecord makeAvailableReward(String sourceEventId, Long beneficiaryUserId, int rewardLevel, String rewardAmount) {
        RewardRecord record = RewardRecord.create(
                sourceEventId,
                beneficiaryUserId,
                71000L + rewardLevel,
                rewardLevel,
                new BigDecimal(rewardAmount),
                new BigDecimal("0.100000"),
                new BigDecimal(rewardAmount),
                "DIAMOND",
                0,
                LocalDateTime.of(2026, 5, 9, 0, 0)
        );
        record.markAvailable();
        return record;
    }
}