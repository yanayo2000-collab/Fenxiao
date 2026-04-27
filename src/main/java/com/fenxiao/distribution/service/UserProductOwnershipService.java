package com.fenxiao.distribution.service;

import com.fenxiao.distribution.entity.UserProductOwnership;
import com.fenxiao.distribution.repository.UserProductOwnershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserProductOwnershipService {

    private final UserProductOwnershipRepository userProductOwnershipRepository;

    public UserProductOwnershipService(UserProductOwnershipRepository userProductOwnershipRepository) {
        this.userProductOwnershipRepository = userProductOwnershipRepository;
    }

    public UserProductOwnership claimOwnership(Long userId,
                                               String productCode,
                                               String ownershipSource,
                                               String sourceRecordType,
                                               Long sourceRecordId) {
        String normalizedProductCode = productCode.trim().toUpperCase(Locale.ROOT);
        String normalizedSource = ownershipSource.trim().toUpperCase(Locale.ROOT);
        String normalizedRecordType = sourceRecordType == null ? null : sourceRecordType.trim().toUpperCase(Locale.ROOT);
        return userProductOwnershipRepository.findByUserIdAndProductCode(userId, normalizedProductCode)
                .map(existing -> {
                    existing.activate(normalizedSource, normalizedRecordType, sourceRecordId);
                    return userProductOwnershipRepository.save(existing);
                })
                .orElseGet(() -> userProductOwnershipRepository.save(
                        UserProductOwnership.create(userId, normalizedProductCode, normalizedSource, normalizedRecordType, sourceRecordId)
                ));
    }
}