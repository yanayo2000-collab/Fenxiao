package com.fenxiao.identity.repository;

import com.fenxiao.identity.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenHash(String tokenHash);
    List<UserSession> findByUserIdAndRevokedAtIsNull(Long userId);
}
