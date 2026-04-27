package com.fenxiao.distribution.repository;

import com.fenxiao.distribution.entity.UserProductOwnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserProductOwnershipRepository extends JpaRepository<UserProductOwnership, Long> {
    Optional<UserProductOwnership> findByUserIdAndProductCode(Long userId, String productCode);

    List<UserProductOwnership> findByUserIdOrderByIdDesc(Long userId);

    @Query("select o.userId from UserProductOwnership o where o.productCode = :productCode and o.ownershipStatus = 'ACTIVE' order by o.userId asc")
    List<Long> findUserIdsByProductCode(String productCode);
}