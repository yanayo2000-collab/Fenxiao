package com.fenxiao.platform.repository;

import com.fenxiao.platform.entity.PlatformAccountBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlatformAccountBindingRepository extends JpaRepository<PlatformAccountBinding, Long> {
    Optional<PlatformAccountBinding> findByUserIdAndPlatformCode(Long userId, String platformCode);
    Optional<PlatformAccountBinding> findByPlatformCodeAndPlatformUserId(String platformCode, String platformUserId);
    List<PlatformAccountBinding> findByUserIdInAndPlatformCode(Collection<Long> userIds, String platformCode);
}
