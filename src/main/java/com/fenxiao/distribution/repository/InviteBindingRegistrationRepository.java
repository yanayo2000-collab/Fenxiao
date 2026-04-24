package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.InviteBindingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteBindingRegistrationRepository extends JpaRepository<InviteBindingRegistration, Long> {
    boolean existsByWhatsappNumber(String whatsappNumber);
    boolean existsByLinkyAccount(String linkyAccount);
}
