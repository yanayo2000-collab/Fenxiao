package com.fenxiao.admin.service;

import com.fenxiao.admin.entity.AdminAccount;
import com.fenxiao.admin.repository.AdminAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountBootstrapper implements ApplicationRunner {
    private final AdminAccountRepository adminAccountRepository;
    private final AdminPasswordHasher passwordHasher;
    private final String bootstrapUsername;
    private final String bootstrapPassword;
    private final String bootstrapDisplayName;

    public AdminAccountBootstrapper(AdminAccountRepository adminAccountRepository,
                                    AdminPasswordHasher passwordHasher,
                                    @Value("${app.admin.bootstrap-username:}") String bootstrapUsername,
                                    @Value("${app.admin.bootstrap-password:}") String bootstrapPassword,
                                    @Value("${app.admin.bootstrap-display-name:Super Admin}") String bootstrapDisplayName) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordHasher = passwordHasher;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapDisplayName = bootstrapDisplayName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminAccountRepository.count() > 0) {
            return;
        }
        if (bootstrapUsername == null || bootstrapUsername.isBlank() || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            return;
        }
        adminAccountRepository.save(AdminAccount.create(
                bootstrapUsername,
                bootstrapDisplayName,
                "super_admin",
                passwordHasher.hash(bootstrapPassword),
                true
        ));
    }
}
