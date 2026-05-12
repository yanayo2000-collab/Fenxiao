package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.distribution.entity.WithdrawRequestItem;
import com.fenxiao.distribution.repository.WithdrawRequestItemRepository;
import com.fenxiao.distribution.repository.WithdrawRequestRepository;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class WithdrawRequestService {

    private static final BigDecimal MIN_WITHDRAW_DIAMOND_AMOUNT = new BigDecimal("1000.000000");

    private final WithdrawRequestRepository withdrawRequestRepository;
    private final WithdrawRequestItemRepository withdrawRequestItemRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final Clock clock;

    @Autowired
    public WithdrawRequestService(WithdrawRequestRepository withdrawRequestRepository,
                                  WithdrawRequestItemRepository withdrawRequestItemRepository,
                                  RewardRecordRepository rewardRecordRepository) {
        this(withdrawRequestRepository, withdrawRequestItemRepository, rewardRecordRepository, Clock.systemUTC());
    }

    WithdrawRequestService(WithdrawRequestRepository withdrawRequestRepository,
                           WithdrawRequestItemRepository withdrawRequestItemRepository,
                           RewardRecordRepository rewardRecordRepository,
                           Clock clock) {
        this.withdrawRequestRepository = withdrawRequestRepository;
        this.withdrawRequestItemRepository = withdrawRequestItemRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.clock = clock;
    }

    public WithdrawRequest createRequest(Long userId) {
        String requestWeek = currentRequestWeek();
        if (withdrawRequestRepository.existsByUserIdAndRequestWeek(userId, requestWeek)) {
            throw new IllegalStateException("withdraw request already submitted this week");
        }

        List<RewardRecord> claimableRewards = rewardRecordRepository
                .findByBeneficiaryUserIdAndRewardStatusAndWithdrawStatusOrderByIdDesc(userId, RewardStatus.AVAILABLE, "UNCLAIMED");
        BigDecimal requestedAmount = claimableRewards.stream()
                .map(RewardRecord::getRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedAmount.compareTo(MIN_WITHDRAW_DIAMOND_AMOUNT) < 0) {
            throw new IllegalStateException("minimum withdraw amount is 1000 diamonds");
        }

        WithdrawRequest request = withdrawRequestRepository.save(WithdrawRequest.create(userId, requestedAmount, requestWeek));
        for (RewardRecord rewardRecord : claimableRewards) {
            rewardRecord.markClaimedInRequest();
            withdrawRequestItemRepository.save(WithdrawRequestItem.create(request.getId(), rewardRecord.getId(), rewardRecord.getRewardAmount()));
        }
        rewardRecordRepository.saveAll(claimableRewards);
        return request;
    }

    public WithdrawRequest approveRequest(String requestNo, Long reviewerId, String remark) {
        WithdrawRequest request = getByRequestNo(requestNo);
        List<RewardRecord> rewardRecords = loadRequestRewards(request);
        request.markPaidOut(reviewerId, remark, now());
        rewardRecords.forEach(RewardRecord::markPaidOut);
        rewardRecordRepository.saveAll(rewardRecords);
        return withdrawRequestRepository.save(request);
    }

    public WithdrawRequest rejectRequest(String requestNo, Long reviewerId, String reason) {
        WithdrawRequest request = getByRequestNo(requestNo);
        List<RewardRecord> rewardRecords = loadRequestRewards(request);
        request.markRejected(reviewerId, reason, now());
        rewardRecords.forEach(RewardRecord::resetWithdrawClaim);
        rewardRecordRepository.saveAll(rewardRecords);
        return withdrawRequestRepository.save(request);
    }

    public Page<WithdrawRequest> listRequests(Long userId, String status, int page, int size) {
        validatePageRequest(page, size);
        return withdrawRequestRepository.findAdminRequests(userId, normalizeStatus(status), PageRequest.of(page, size));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private WithdrawRequest getByRequestNo(String requestNo) {
        return withdrawRequestRepository.findByRequestNo(requestNo)
                .orElseThrow(() -> new IllegalArgumentException("withdraw request not found"));
    }

    private List<RewardRecord> loadRequestRewards(WithdrawRequest request) {
        List<Long> rewardIds = withdrawRequestItemRepository.findByWithdrawRequestId(request.getId()).stream()
                .map(WithdrawRequestItem::getRewardRecordId)
                .toList();
        return rewardRecordRepository.findAllById(rewardIds);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String currentRequestWeek() {
        LocalDate today = LocalDate.now(clock);
        WeekFields weekFields = WeekFields.ISO;
        int weekBasedYear = today.get(weekFields.weekBasedYear());
        int week = today.get(weekFields.weekOfWeekBasedYear());
        return "%d-W%02d".formatted(weekBasedYear, week);
    }
}
