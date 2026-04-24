package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.CreateInviteBindingRequest;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.distribution.entity.InviteBindingRegistration;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
import com.fenxiao.distribution.repository.InviteBindingRegistrationRepository;
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

    @Test
    void shouldRegisterInviteBindingWithUniqueWhatsappAndLinkyAccount() {
        UserDistributionProfile inviter = distributionBindingService.createProfile(51001L, "ID", "id", null);

        InviteBindingRegistration registration = inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                inviter.getInviteCode().toLowerCase(),
                "+62 81234567890",
                "12345678"
        ));

        assertThat(registration.getInviterUserId()).isEqualTo(inviter.getUserId());
        assertThat(registration.getInviteCode()).isEqualTo(inviter.getInviteCode());
        assertThat(registration.getWhatsappNumber()).isEqualTo("+6281234567890");
        assertThat(registration.getLinkyAccount()).isEqualTo("12345678");
        assertThat(registration.getBindStatus()).isEqualTo("ACTIVE");
        assertThat(inviteBindingRegistrationRepository.findById(registration.getId())).isPresent();
        assertThat(userDistributionProfileRepository.findById(12345678L)).isPresent();
        DistributionRelation relation = distributionRelationRepository.findByUserId(12345678L).orElseThrow();
        assertThat(relation.getLevel1InviterId()).isEqualTo(inviter.getUserId());
    }

    @Test
    void shouldRejectDuplicateWhatsappOrLinkyAccount() {
        UserDistributionProfile inviter = distributionBindingService.createProfile(51002L, "ID", "id", null);
        inviteBindingRegistrationService.register(new CreateInviteBindingRequest(inviter.getInviteCode(), "+6281234567890", "87654321"));

        assertThatThrownBy(() -> inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                inviter.getInviteCode(),
                "+6281234567890",
                "12345678"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("whatsapp number already registered");

        assertThatThrownBy(() -> inviteBindingRegistrationService.register(new CreateInviteBindingRequest(
                inviter.getInviteCode(),
                "+6281111111111",
                "87654321"
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("linky account already registered");
    }
}
