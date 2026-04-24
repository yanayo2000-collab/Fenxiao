package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.CreateInviteBindingRequest;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.entity.InviteBindingRegistration;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.repository.InviteBindingRegistrationRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Transactional
public class InviteBindingRegistrationService {

    private final UserDistributionProfileRepository userDistributionProfileRepository;
    private final InviteBindingRegistrationRepository inviteBindingRegistrationRepository;
    private final DistributionRelationRepository distributionRelationRepository;
    private final DistributionBindingService distributionBindingService;

    public InviteBindingRegistrationService(UserDistributionProfileRepository userDistributionProfileRepository,
                                            InviteBindingRegistrationRepository inviteBindingRegistrationRepository,
                                            DistributionRelationRepository distributionRelationRepository,
                                            DistributionBindingService distributionBindingService) {
        this.userDistributionProfileRepository = userDistributionProfileRepository;
        this.inviteBindingRegistrationRepository = inviteBindingRegistrationRepository;
        this.distributionRelationRepository = distributionRelationRepository;
        this.distributionBindingService = distributionBindingService;
    }

    public InviteBindingRegistration register(CreateInviteBindingRequest request) {
        String normalizedInviteCode = normalizeInviteCode(request.inviteCode());
        String normalizedWhatsappNumber = normalizeWhatsappNumber(request.whatsappNumber());
        String normalizedLinkyAccount = normalizeLinkyAccount(request.linkyAccount());

        UserDistributionProfile inviter = userDistributionProfileRepository.findByInviteCode(normalizedInviteCode)
                .orElseThrow(() -> new IllegalArgumentException("invite code not found"));

        if (inviteBindingRegistrationRepository.existsByWhatsappNumber(normalizedWhatsappNumber)) {
            throw new IllegalStateException("whatsapp number already registered");
        }
        if (inviteBindingRegistrationRepository.existsByLinkyAccount(normalizedLinkyAccount)) {
            throw new IllegalStateException("linky account already registered");
        }

        InviteBindingRegistration registration = InviteBindingRegistration.createActive(
                inviter.getUserId(),
                inviter.getInviteCode(),
                normalizedWhatsappNumber,
                normalizedLinkyAccount
        );
        InviteBindingRegistration saved = inviteBindingRegistrationRepository.save(registration);
        Long inviteeUserId = Long.valueOf(normalizedLinkyAccount);
        if (userDistributionProfileRepository.existsById(inviteeUserId)) {
            DistributionRelation existingRelation = distributionRelationRepository.findByUserId(inviteeUserId)
                    .orElseThrow(() -> new IllegalStateException("distribution relation not found"));
            if (existingRelation.getLevel1InviterId() != null) {
                throw new IllegalStateException("user already has inviter");
            }
            distributionRelationRepository.delete(existingRelation);
            distributionBindingService.bindInviter(inviteeUserId, inviter.getInviteCode());
        } else {
            distributionBindingService.createProfile(inviteeUserId, inviter.getCountryCode(), inviter.getLanguageCode(), inviter.getInviteCode());
        }
        return saved;
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeWhatsappNumber(String whatsappNumber) {
        return whatsappNumber.replaceAll("[\\s()-]", "").trim();
    }

    private String normalizeLinkyAccount(String linkyAccount) {
        String normalized = linkyAccount.replaceAll("\\s+", "").trim();
        if (!normalized.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("linky account must be 8 digits");
        }
        return normalized;
    }
}
