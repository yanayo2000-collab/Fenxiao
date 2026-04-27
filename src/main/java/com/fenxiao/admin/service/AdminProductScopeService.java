package com.fenxiao.admin.service;

import com.fenxiao.distribution.repository.UserProductOwnershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AdminProductScopeService {

    private final UserProductOwnershipRepository userProductOwnershipRepository;

    public AdminProductScopeService(UserProductOwnershipRepository userProductOwnershipRepository) {
        this.userProductOwnershipRepository = userProductOwnershipRepository;
    }

    public String normalizeProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return null;
        }
        String normalized = productCode.trim().toUpperCase(Locale.ROOT);
        return "ALL".equals(normalized) ? null : normalized;
    }

    public List<Long> resolveScopedUserIds(String productCode) {
        String normalized = normalizeProductCode(productCode);
        if (normalized == null) {
            return List.of();
        }
        return userProductOwnershipRepository.findUserIdsByProductCode(normalized);
    }
}
