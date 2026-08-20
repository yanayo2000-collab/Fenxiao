package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.LinkyAccountBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LinkyAccountBindingRepository extends JpaRepository<LinkyAccountBinding, Long> {
    Optional<LinkyAccountBinding> findByLinkyAccount(String linkyAccount);

    Page<LinkyAccountBinding> findByUserIdIsNotNullOrderByIdAsc(Pageable pageable);

    @Query("select b.userId from LinkyAccountBinding b where b.guildId = :guildId and b.userId is not null")
    List<Long> findRegisteredUserIdsByGuildId(String guildId);
}
