package com.fenxiao.admin.service;

import java.util.Set;

public enum AdminPermission {
    READ(Set.of("super_admin", "admin", "operator", "operations", "mentor", "team_leader", "finance", "customer_support")),
    WRITE(Set.of("super_admin", "admin", "operations")),
    MENTOR_MANAGE(Set.of("super_admin", "admin", "operations")),
    TEAM_MANAGE(Set.of("super_admin", "admin", "operations")),
    FINANCE(Set.of("super_admin", "finance")),
    CUSTOMER_SUPPORT(Set.of("super_admin", "customer_support")),
    ACCOUNT_MANAGE(Set.of("super_admin"));

    private final Set<String> roles;

    AdminPermission(Set<String> roles) {
        this.roles = roles;
    }

    public boolean allows(String role) {
        return role != null && roles.contains(role.toLowerCase());
    }
}
