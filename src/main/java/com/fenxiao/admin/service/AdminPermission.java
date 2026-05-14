package com.fenxiao.admin.service;

import java.util.Set;

public enum AdminPermission {
    READ(Set.of("super_admin", "admin", "operator")),
    WRITE(Set.of("super_admin", "admin")),
    ACCOUNT_MANAGE(Set.of("super_admin"));

    private final Set<String> roles;

    AdminPermission(Set<String> roles) {
        this.roles = roles;
    }

    public boolean allows(String role) {
        return role != null && roles.contains(role.toLowerCase());
    }
}
