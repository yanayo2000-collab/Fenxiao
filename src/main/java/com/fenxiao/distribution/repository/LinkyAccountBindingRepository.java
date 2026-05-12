package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.LinkyAccountBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkyAccountBindingRepository extends JpaRepository<LinkyAccountBinding, Long> {
    Optional<LinkyAccountBinding> findByLinkyAccount(String linkyAccount);
}
