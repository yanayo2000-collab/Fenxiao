package com.fenxiao.distribution.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LinkyEligibilityRefreshScheduler {

    private final LinkyRegistrationEligibilityService linkyRegistrationEligibilityService;

    public LinkyEligibilityRefreshScheduler(LinkyRegistrationEligibilityService linkyRegistrationEligibilityService) {
        this.linkyRegistrationEligibilityService = linkyRegistrationEligibilityService;
    }

    @Scheduled(cron = "0 0 */6 * * *")
    public void refreshAllLinkyEligibility() {
        linkyRegistrationEligibilityService.refreshAllEligibility();
    }
}
