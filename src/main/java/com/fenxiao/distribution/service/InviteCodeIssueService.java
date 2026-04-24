package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.IssueInviteCodeRequest;
import com.fenxiao.distribution.entity.InviteCodeIssueRecord;
import com.fenxiao.distribution.repository.InviteCodeIssueRecordRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class InviteCodeIssueService {

    private final InviteCodeIssueRecordRepository inviteCodeIssueRecordRepository;
    private final DistributionBindingService distributionBindingService;

    public InviteCodeIssueService(InviteCodeIssueRecordRepository inviteCodeIssueRecordRepository,
                                  DistributionBindingService distributionBindingService) {
        this.inviteCodeIssueRecordRepository = inviteCodeIssueRecordRepository;
        this.distributionBindingService = distributionBindingService;
    }

    public InviteCodeIssueResult issue(IssueInviteCodeRequest request) {
        String productCode = normalizeProductCode(request.productCode());
        String whatsappNumber = normalizeWhatsappNumber(request.whatsappNumber());
        String appAccount = normalizeAppAccount(request.appAccount());
        Long userId = Long.valueOf(appAccount);

        inviteCodeIssueRecordRepository.findByWhatsappNumber(whatsappNumber).ifPresent(existing -> {
            if (!existing.getAppAccount().equals(appAccount)) {
                throw new IllegalStateException("whatsapp number already issued");
            }
        });
        inviteCodeIssueRecordRepository.findByAppAccount(appAccount).ifPresent(existing -> {
            if (!existing.getWhatsappNumber().equals(whatsappNumber)) {
                throw new IllegalStateException("app account already issued");
            }
        });

        UserDistributionProfile profile = distributionBindingService.ensureRootProfile(userId, "ID", "id");
        InviteCodeIssueRecord record = inviteCodeIssueRecordRepository.findByIssuerUserId(userId)
                .orElseGet(() -> inviteCodeIssueRecordRepository.save(InviteCodeIssueRecord.create(
                        profile.getUserId(),
                        productCode,
                        whatsappNumber,
                        appAccount,
                        profile.getInviteCode()
                )));

        return new InviteCodeIssueResult(record, profile);
    }

    private String normalizeProductCode(String productCode) {
        return productCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeWhatsappNumber(String whatsappNumber) {
        return whatsappNumber.replaceAll("[\\s()-]", "").trim();
    }

    private String normalizeAppAccount(String appAccount) {
        return appAccount.trim();
    }

    public record InviteCodeIssueResult(InviteCodeIssueRecord record, UserDistributionProfile profile) {
    }
}