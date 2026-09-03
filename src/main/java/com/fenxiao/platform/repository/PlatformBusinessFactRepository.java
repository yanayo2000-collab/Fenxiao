package com.fenxiao.platform.repository;

import com.fenxiao.platform.entity.PlatformBusinessFact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlatformBusinessFactRepository extends JpaRepository<PlatformBusinessFact, Long> {
    boolean existsBySourceSystemAndSourceEventId(String sourceSystem, String sourceEventId);
    List<PlatformBusinessFact> findByUserIdAndPlatformCodeOrderByOccurredAtAscIdAsc(Long userId, String platformCode);
}

