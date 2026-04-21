package com.fenxiao.distribution.service;

import com.fenxiao.distribution.domain.BindSource;
import com.fenxiao.distribution.entity.DistributionRelation;
import com.fenxiao.user.entity.UserDistributionProfile;
import com.fenxiao.user.repository.UserDistributionProfileRepository;
import com.fenxiao.distribution.repository.DistributionRelationRepository;
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
class DistributionBindingServiceTest {

    @Autowired
    private DistributionBindingService distributionBindingService;

    @Autowired
    private UserDistributionProfileRepository userProfileRepository;

    @Autowired
    private DistributionRelationRepository relationRepository;

    @Test
    void shouldGenerateInviteCodeWhenCreatingProfile() {
        UserDistributionProfile profile = distributionBindingService.createProfile(1001L, "ID", "id", null);

        assertThat(profile.getUserId()).isEqualTo(1001L);
        assertThat(profile.getInviteCode()).isNotBlank();
        assertThat(profile.getInviteCode()).hasSize(8);
        assertThat(profile.getCountryCode()).isEqualTo("ID");
    }

    @Test
    void shouldBindUserToInviterAndBuildThreeLevelChain() {
        UserDistributionProfile root = distributionBindingService.createProfile(2001L, "ID", "id", null);
        UserDistributionProfile level1 = distributionBindingService.createProfile(2002L, "ID", "id", root.getInviteCode());
        UserDistributionProfile level2 = distributionBindingService.createProfile(2003L, "ID", "id", level1.getInviteCode());
        UserDistributionProfile level3 = distributionBindingService.createProfile(2004L, "ID", "id", level2.getInviteCode());

        DistributionRelation relation = relationRepository.findByUserId(level3.getUserId()).orElseThrow();

        assertThat(relation.getLevel1InviterId()).isEqualTo(level2.getUserId());
        assertThat(relation.getLevel2InviterId()).isEqualTo(level1.getUserId());
        assertThat(relation.getLevel3InviterId()).isEqualTo(root.getUserId());
        assertThat(relation.getBindSource()).isEqualTo(BindSource.INVITE_CODE);
    }

    @Test
    void shouldRejectSelfBindingAndDuplicateBinding() {
        UserDistributionProfile inviter = userProfileRepository.save(UserDistributionProfile.create(3001L, "BR", "pt", "SELF3001"));

        assertThatThrownBy(() -> distributionBindingService.bindInviter(3001L, inviter.getInviteCode()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot bind to self");

        distributionBindingService.createProfile(3002L, "BR", "pt", null);
        UserDistributionProfile otherInviter = distributionBindingService.createProfile(3003L, "BR", "pt", null);

        assertThatThrownBy(() -> distributionBindingService.bindInviter(3002L, otherInviter.getInviteCode()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has inviter");
    }

    @Test
    void shouldRejectLateBindingForRootUser() {
        distributionBindingService.createProfile(4001L, "BR", "pt", null);
        UserDistributionProfile inviter = distributionBindingService.createProfile(4002L, "BR", "pt", null);

        assertThatThrownBy(() -> distributionBindingService.bindInviter(4001L, inviter.getInviteCode().toLowerCase()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has inviter");
    }
}

