package com.fenxiao.distribution.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendVerificationCode(String phoneNumber, String verificationCode, int ttlMinutes) {
        log.info("phone verification code issued for phone={} ttlMinutes={}", maskPhone(phoneNumber), ttlMinutes);
    }

    private String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 6) return "[REDACTED]";
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 3);
    }
}
