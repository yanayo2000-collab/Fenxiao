package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.CreateInviteBindingRequest;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.entity.InviteBindingRegistration;
import com.fenxiao.distribution.entity.UserProductOwnership;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.repository.InviteBindingRegistrationRepository;
import com.fenxiao.distribution.repository.UserProductOwnershipRepository;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class InviteBindingRegistrationServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private InviteBindingRegistrationService inviteBindingRegistrationService;

    @Autowired
    private InviteBindingRegistrationRepository inviteBindingRegistrationRepository;

    @Autowired
    private DistributionRelationRepository distributionRelationRepository;

    @Autowired
    private UserDistributionProfileRepository userDistributionProfileRepository;

    @Autowired
    private UserProductOwnershipRepository userProductOwnershipRepository;

    @Test
    void shouldRegisterInviteBindingWithProductCodeAndUniqueWhatsappAndLinkyAccount() {
        UserDistributionProfile inviter = distributionBindingService.createProfile(51001L, "ID", "id", null);

        InviteBindingRegistration registration = inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                "LINKY",
                inviter.getInviteCode().toLowerCase(),
                "+6281234567890",
                "12345678"
        ));

        assertThat(registration.getInviterUserId()).isEqualTo(inviter.getUserId());
        assertThat(registration.getProductCode()).isEqualTo("LINKY");
        assertThat(registration.getInviteCode()).isEqualTo(inviter.getInviteCode());
        assertThat(registration.getWhatsappNumber()).isEqualTo("+6281234567890");
        assertThat(registration.getLinkyAccount()).isEqualTo("12345678");
        assertThat(registration.getBindStatus()).isEqualTo("ACTIVE");
        assertThat(inviteBindingRegistrationRepository.findById(registration.getId())).isPresent();
        assertThat(userDistributionProfileRepository.findById(12345678L)).isPresent();
        DistributionRelation relation = distributionRelationRepository.findByUserId(12345678L).orElseThrow();
        assertThat(relation.getLevel1InviterId()).isEqualTo(inviter.getUserId());
        UserProductOwnership ownership = userProductOwnershipRepository.findByUserIdAndProductCode(12345678L, "LINKY").orElseThrow();
        assertThat(ownership.getOwnershipSource()).isEqualTo("INVITE_BINDING");
        assertThat(ownership.getSourceRecordType()).isEqualTo("INVITE_BINDING_REGISTRATION");
        assertThat(ownership.getSourceRecordId()).isEqualTo(registration.getId());
    }

    @Test
    void shouldRejectDuplicateWhatsappOrLinkyAccount() {
        UserDistributionProfile inviter = distributionBindingService.createProfile(51002L, "ID", "id", null);
        inviteBindingRegistrationService.register(new CreateInviteBindingRequest("LINKY", inviter.getInviteCode(), "+6281234500001", "87654321"));

        assertThatThrownBy(() -> inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                "LINKY",
                inviter.getInviteCode(),
                "+6281234500001",
                "12345678"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("whatsapp number already registered");

        assertThatThrownBy(() -> inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                "LINKY",
                inviter.getInviteCode(),
                "+628****1111",
                "87654321"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("linky account already registered");
    }
}
