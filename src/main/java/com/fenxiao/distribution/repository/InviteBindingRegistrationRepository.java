package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.InviteBindingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InviteBindingRegistrationRepository extends JpaRepository<InviteBindingRegistration, Long> {
    boolean existsByWhatsappNumber(String whatsappNumber);
    boolean existsByLinkyAccount(String linkyAccount);

    @Query("select r.linkyAccount from InviteBindingRegistration r where r.productCode = :productCode order by r.linkyAccount asc")
    List<String> findLinkyAccountsByProductCode(String productCode);
}
