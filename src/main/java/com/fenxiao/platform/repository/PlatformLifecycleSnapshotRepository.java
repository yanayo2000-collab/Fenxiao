package com.fenxiao.platform.repository;

import com.fenxiao.platform.entity.PlatformLifecycleSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface PlatformLifecycleSnapshotRepository extends JpaRepository<PlatformLifecycleSnapshot, Long> {
    Optional<PlatformLifecycleSnapshot> findByUserIdAndPlatformCode(Long userId, String platformCode);
    List<PlatformLifecycleSnapshot> findByUserIdInAndPlatformCode(Collection<Long> userIds, String platformCode);
}
