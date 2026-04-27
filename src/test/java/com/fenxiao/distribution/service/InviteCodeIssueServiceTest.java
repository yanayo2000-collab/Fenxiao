package com.fenxiao.distribution.service;

import com.fenxiao.distribution.api.dto.IssueInviteCodeRequest;
import com.fenxiao.distribution.entity.UserProductOwnership;
import com.fenxiao.distribution.repository.InviteCodeIssueRecordRepository;
import com.fenxiao.distribution.repository.UserProductOwnershipRepository;
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
class InviteCodeIssueServiceTest {

    @Autowired
    private InviteCodeIssueService inviteCodeIssueService;

    @Autowired
    private InviteCodeIssueRecordRepository inviteCodeIssueRecordRepository;

    @Autowired
    private UserProductOwnershipRepository userProductOwnershipRepository;

    @Test
    void shouldIssueInviteCodeUsingProductWhatsappAndAppAccount() {
        InviteCodeIssueService.InviteCodeIssueResult result = inviteCodeIssueService.issue(new IssueInviteCodeRequest(
                "linky",
                "+62 81234567890",
                "12345678"
        ));

        assertThat(result.profile().getUserId()).isEqualTo(12345678L);
        assertThat(result.profile().getInviteCode()).isNotBlank();
        assertThat(result.record().getProductCode()).isEqualTo("LINKY");
        assertThat(result.record().getWhatsappNumber()).endsWith("7890");
        assertThat(result.record().getAppAccount()).isEqualTo("12345678");
        assertThat(inviteCodeIssueRecordRepository.findByIssuerUserId(12345678L)).isPresent();
        UserProductOwnership ownership = userProductOwnershipRepository.findByUserIdAndProductCode(12345678L, "LINKY").orElseThrow();
        assertThat(ownership.getOwnershipSource()).isEqualTo("INVITE_CODE_ISSUE");
        assertThat(ownership.getSourceRecordType()).isEqualTo("INVITE_CODE_ISSUE_RECORD");
        assertThat(ownership.getSourceRecordId()).isEqualTo(result.record().getId());
    }

    @Test
    void shouldRejectDuplicateWhatsappOrAppAccountAcrossDifferentOwners() {
        inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****7890", "12345678"));

        assertThatThrownBy(() -> inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****7890", "87654321")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("whatsapp number already issued");

        assertThatThrownBy(() -> inviteCodeIssueService.issue(new IssueInviteCodeRequest("LINKY", "+628****0001", "12345678")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app account already issued");
    }
}
