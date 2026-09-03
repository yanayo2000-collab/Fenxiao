package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.WithdrawRequest;
import com.fenxiao.distribution.entity.WithdrawRequestItem;
import com.fenxiao.distribution.repository.WithdrawRequestItemRepository;
import com.fenxiao.distribution.repository.WithdrawRequestRepository;
import com.fenxiao.audit.entity.OperationAuditLog;
import com.fenxiao.audit.repository.OperationAuditLogRepository;
import com.fenxiao.reward.domain.RewardStatus;
import com.fenxiao.reward.domain.RewardType;
import com.fenxiao.reward.entity.RewardRecord;
import com.fenxiao.reward.repository.RewardRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@Transactional
public class WithdrawRequestService {

    private static final BigDecimal MIN_WITHDRAW_DIAMOND_AMOUNT = new BigDecimal("1000.000000");

    private final WithdrawRequestRepository withdrawRequestRepository;
    private final WithdrawRequestItemRepository withdrawRequestItemRepository;
    private final RewardRecordRepository rewardRecordRepository;
    private final OperationAuditLogRepository operationAuditLogRepository;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Autowired
    public WithdrawRequestService(WithdrawRequestRepository withdrawRequestRepository,
                                  WithdrawRequestItemRepository withdrawRequestItemRepository,
                                  RewardRecordRepository rewardRecordRepository,
                                  OperationAuditLogRepository operationAuditLogRepository,
                                  JdbcTemplate jdbc) {
        this(withdrawRequestRepository, withdrawRequestItemRepository, rewardRecordRepository, operationAuditLogRepository, jdbc, Clock.systemUTC());
    }

    WithdrawRequestService(WithdrawRequestRepository withdrawRequestRepository,
                           WithdrawRequestItemRepository withdrawRequestItemRepository,
                           RewardRecordRepository rewardRecordRepository,
                           OperationAuditLogRepository operationAuditLogRepository,
                           JdbcTemplate jdbc,
                           Clock clock) {
        this.withdrawRequestRepository = withdrawRequestRepository;
        this.withdrawRequestItemRepository = withdrawRequestItemRepository;
        this.rewardRecordRepository = rewardRecordRepository;
        this.operationAuditLogRepository = operationAuditLogRepository;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public WithdrawRequest createRequest(Long userId) {
        String requestWeek = currentRequestWeek();
        if (withdrawRequestRepository.existsByUserIdAndRequestWeek(userId, requestWeek)) {
            throw new IllegalStateException("withdraw request already submitted this week");
        }

        List<RewardRecord> claimableRewards = rewardRecordRepository
                .findByBeneficiaryUserIdAndRewardStatusAndWithdrawStatusAndRewardTypeAndRewardLevelOrderByIdDesc(
                        userId,
                        RewardStatus.AVAILABLE,
                        "UNCLAIMED",
                        RewardType.DIRECT_RECRUIT,
                        1
                );
        BigDecimal requestedAmount = claimableRewards.stream()
                .map(RewardRecord::getRewardAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedAmount.compareTo(MIN_WITHDRAW_DIAMOND_AMOUNT) < 0) {
            throw new IllegalStateException("minimum withdraw amount is 1000 diamonds");
        }

        WithdrawRequest request = withdrawRequestRepository.save(WithdrawRequest.create(userId, requestedAmount, requestWeek));
        transition(request, null, "PENDING_REVIEW", userId, "USER", "withdrawal submitted");
        for (RewardRecord rewardRecord : claimableRewards) {
            rewardRecord.markClaimedInRequest();
            withdrawRequestItemRepository.save(WithdrawRequestItem.create(request.getId(), rewardRecord.getId(), rewardRecord.getRewardAmount()));
        }
        rewardRecordRepository.saveAll(claimableRewards);
        return request;
    }

    public WithdrawRequest approveRequest(String requestNo, Long reviewerId, String reviewerRole, String remark) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String before = snapshot(request);
        List<RewardRecord> rewardRecords = loadRequestRewards(request);
        request.markPaidOut(reviewerId, remark, now());
        rewardRecords.forEach(RewardRecord::markPaidOutFromExistingRequest);
        rewardRecordRepository.saveAll(rewardRecords);
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        transition(saved, "PENDING_REVIEW", "PAID_OUT", reviewerId, reviewerRole, remark);
        audit(saved, reviewerId, reviewerRole, "APPROVE", before, snapshot(saved), remark);
        return saved;
    }

    public WithdrawRequest approveRequest(String requestNo, Long reviewerId, String remark) {
        return approveRequest(requestNo, reviewerId, "ADMIN_SESSION", remark);
    }

    public WithdrawRequest rejectRequest(String requestNo, Long reviewerId, String reviewerRole, String reason) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String before = snapshot(request);
        List<RewardRecord> rewardRecords = loadRequestRewards(request);
        request.markRejected(reviewerId, reason, now());
        rewardRecords.forEach(RewardRecord::resetWithdrawClaimFromExistingRequest);
        rewardRecordRepository.saveAll(rewardRecords);
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        transition(saved, "PENDING_REVIEW", "REJECTED", reviewerId, reviewerRole, reason);
        audit(saved, reviewerId, reviewerRole, "REJECT", before, snapshot(saved), reason);
        return saved;
    }

    public WithdrawRequest rejectRequest(String requestNo, Long reviewerId, String reason) {
        return rejectRequest(requestNo, reviewerId, "ADMIN_SESSION", reason);
    }

    public WithdrawRequest approveForPayment(String requestNo, Long reviewerId, String reviewerRole, String remark) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String before = snapshot(request);
        request.approveForPayment(reviewerId, remark, now());
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        transition(saved, "PENDING_REVIEW", "PAYMENT_PENDING", reviewerId, reviewerRole, remark);
        audit(saved, reviewerId, reviewerRole, "APPROVE_FOR_PAYMENT", before, snapshot(saved), remark);
        return saved;
    }

    public WithdrawRequest recordPaymentSuccess(String requestNo, Long operatorId, String operatorRole,
                                                String channel, String reference, String evidenceUri, String evidenceHash) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String from = request.getRequestStatus();
        String before = snapshot(request);
        int attemptNo = nextAttemptNo(request.getId());
        request.markPaymentSucceeded(operatorId, channel, reference, evidenceUri, evidenceHash, now());
        List<RewardRecord> rewards = loadRequestRewards(request);
        rewards.forEach(RewardRecord::markPaidOutFromExistingRequest);
        rewardRecordRepository.saveAll(rewards);
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        insertPaymentAttempt(saved, attemptNo, channel, reference, evidenceUri, evidenceHash, "SUCCESS", null, operatorId);
        transition(saved, from, "PAID_OUT", operatorId, operatorRole, reference);
        audit(saved, operatorId, operatorRole, "PAYMENT_SUCCESS", before, snapshot(saved), reference);
        return saved;
    }

    public WithdrawRequest recordPaymentFailure(String requestNo, Long operatorId, String operatorRole,
                                                String channel, String reference, String evidenceUri, String evidenceHash,
                                                String failureReason) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String before = snapshot(request);
        int attemptNo = nextAttemptNo(request.getId());
        request.markPaymentFailed(failureReason);
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        insertPaymentAttempt(saved, attemptNo, channel, reference, evidenceUri, evidenceHash, "FAILED", failureReason, operatorId);
        transition(saved, "PAYMENT_PENDING", "PAYMENT_FAILED", operatorId, operatorRole, failureReason);
        audit(saved, operatorId, operatorRole, "PAYMENT_FAILED", before, snapshot(saved), failureReason);
        return saved;
    }

    public WithdrawRequest reversePayment(String requestNo, Long operatorId, String operatorRole, String reason, String currencyCode) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String before = snapshot(request);
        request.markReversed(operatorId, reason, now());
        jdbc.update("insert into withdraw_reversal_ledger(withdraw_request_id,reversal_amount,currency_code,reason,reversed_by,reversed_at) values(?,?,?,?,?,?)",
                request.getId(), request.getApprovedDiamondAmount(), normalizeCurrency(currencyCode), reason.trim(), operatorId, now());
        WithdrawRequest saved = withdrawRequestRepository.save(request);
        transition(saved, "PAID_OUT", "REVERSED", operatorId, operatorRole, reason);
        audit(saved, operatorId, operatorRole, "REVERSE", before, snapshot(saved), reason);
        return saved;
    }

    public void reconcile(String requestNo, Long operatorId, String status, String externalReference,
                          BigDecimal externalAmount, String currencyCode, String details) {
        WithdrawRequest request = getByRequestNo(requestNo);
        String normalized = normalizeReconciliationStatus(status);
        jdbc.update("insert into withdraw_reconciliation_record(withdraw_request_id,reconciliation_status,external_reference,external_amount,currency_code,details,reconciled_by,reconciled_at) values(?,?,?,?,?,?,?,?)",
                request.getId(), normalized, normalizeText(externalReference), externalAmount,
                currencyCode == null || currencyCode.isBlank() ? null : normalizeCurrency(currencyCode), normalizeText(details), operatorId, now());
    }

    public List<Map<String, Object>> workflowHistory(String requestNo) {
        WithdrawRequest request = getByRequestNo(requestNo);
        return jdbc.queryForList("select from_status,to_status,actor_id,actor_role,reason,occurred_at from withdraw_request_transition where withdraw_request_id=? order by id", request.getId());
    }

    private void audit(WithdrawRequest request, Long reviewerId, String reviewerRole, String action, String before, String after, String remark) {
        if (operationAuditLogRepository == null) return;
        operationAuditLogRepository.save(OperationAuditLog.create(
                reviewerId == null ? 0L : reviewerId,
                normalizeReviewerRole(reviewerRole),
                "withdraw_request",
                "withdraw_request",
                request.getId(),
                action,
                before,
                after,
                null,
                remark,
                now()
        ));
    }

    private void transition(WithdrawRequest request, String from, String to, Long actorId, String actorRole, String reason) {
        if (jdbc == null) return;
        jdbc.update("insert into withdraw_request_transition(withdraw_request_id,from_status,to_status,actor_id,actor_role,reason,occurred_at) values(?,?,?,?,?,?,?)",
                request.getId(), from, to, actorId, normalizeReviewerRole(actorRole), normalizeText(reason), now());
    }

    private int nextAttemptNo(Long requestId) {
        return jdbc.queryForObject("select coalesce(max(attempt_no),0)+1 from withdraw_payment_attempt where withdraw_request_id=?", Integer.class, requestId);
    }

    private void insertPaymentAttempt(WithdrawRequest request, int attemptNo, String channel, String reference,
                                      String evidenceUri, String evidenceHash, String status, String failureReason, Long operatorId) {
        jdbc.update("insert into withdraw_payment_attempt(withdraw_request_id,attempt_no,payment_channel,payment_reference,evidence_uri,evidence_hash,attempt_status,failure_reason,operated_by,operated_at) values(?,?,?,?,?,?,?,?,?,?)",
                request.getId(), attemptNo, channel.trim().toUpperCase(Locale.ROOT), normalizeText(reference), normalizeText(evidenceUri), normalizeText(evidenceHash), status, normalizeText(failureReason), operatorId, now());
    }

    private String normalizeReconciliationStatus(String status) {
        if (status == null) throw new IllegalArgumentException("reconciliation status is required");
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("MATCHED", "MISMATCH", "MISSING_EXTERNAL", "MISSING_INTERNAL").contains(value)) throw new IllegalArgumentException("reconciliation status is invalid");
        return value;
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) return "DIAMOND";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String snapshot(WithdrawRequest request) {
        return "requestNo=" + request.getRequestNo()
                + ",userId=" + request.getUserId()
                + ",status=" + request.getRequestStatus()
                + ",requested=" + request.getRequestedDiamondAmount()
                + ",approved=" + request.getApprovedDiamondAmount();
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

    private String normalizeReviewerRole(String reviewerRole) {
        if (reviewerRole == null || reviewerRole.isBlank()) {
            return "ADMIN_SESSION";
        }
        return reviewerRole.trim().toUpperCase(Locale.ROOT);
    }

    private String currentRequestWeek() {
        LocalDate today = LocalDate.now(clock);
        WeekFields weekFields = WeekFields.ISO;
        int weekBasedYear = today.get(weekFields.weekBasedYear());
        int week = today.get(weekFields.weekOfWeekBasedYear());
        return "%d-W%02d".formatted(weekBasedYear, week);
    }
}
