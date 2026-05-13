package com.fenxiao.distribution.service;

public interface SmsSender {
    void sendVerificationCode(String phoneNumber, String verificationCode, int ttlMinutes);
}
