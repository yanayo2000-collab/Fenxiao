package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.LinkyAccountBinding;
import com.fenxiao.distribution.repository.LinkyAccountBindingRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkyRegistrationEligibilityServiceTest {

    @Test
    void shouldAutoRefreshEligibilityFromGuildProbeWhenNoLocalBindingExists() {
        LinkyAccountBindingRepository repository = mock(LinkyAccountBindingRepository.class);
        LinkyGuildProbeClient probeClient = mock(LinkyGuildProbeClient.class);
        when(repository.findByLinkyAccount("12345678")).thenReturn(Optional.empty());
        when(repository.save(any(LinkyAccountBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(probeClient.probe("12345678")).thenReturn(LinkyGuildProbeResult.matchedOurs("12345678", "413", "Permata", "probe matched"));

        LinkyRegistrationEligibilityService service = new LinkyRegistrationEligibilityService(repository, probeClient);

        LinkyAccountBinding binding = service.assertEligibleForRegistration("12345678");

        assertThat(binding.getRegistrationEligibility()).isEqualTo("ELIGIBLE");
        assertThat(binding.getGuildCheckStatus()).isEqualTo("MATCHED_OURS");
        assertThat(binding.getGuildId()).isEqualTo("413");
        verify(repository).save(any(LinkyAccountBinding.class));
    }

    @Test
    void shouldRejectRegistrationWhenGuildProbeMarksJoinedOtherGuild() {
        LinkyAccountBindingRepository repository = mock(LinkyAccountBindingRepository.class);
        LinkyGuildProbeClient probeClient = mock(LinkyGuildProbeClient.class);
        when(repository.findByLinkyAccount("34567890")).thenReturn(Optional.empty());
        when(repository.save(any(LinkyAccountBinding.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(probeClient.probe("34567890")).thenReturn(LinkyGuildProbeResult.joinedOtherGuild("34567890", "999", "Other Guild", "probe found other guild"));

        LinkyRegistrationEligibilityService service = new LinkyRegistrationEligibilityService(repository, probeClient);

        assertThatThrownBy(() -> service.assertEligibleForRegistration("34567890"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("linky account is not eligible for registration");

        verify(repository).save(any(LinkyAccountBinding.class));
    }
}
