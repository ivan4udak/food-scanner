package com.foodscanner.application.service;

import com.foodscanner.application.command.AdminResetPasswordCommand;
import com.foodscanner.application.usecase.AdminUseCase;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.exception.InvalidAdminCredentialsException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;

/**
 * Слой: application
 * Админский сброс пароля. Роль и админ-пароль приходят из конфигурации
 * (ADMIN_PASSWORD), не хардкодятся — передаются в конструктор.
 */
public class AdminService implements AdminUseCase {

    private static final String ADMIN_ROLE = "volkov";

    private final ContributorRepository repository;
    private final String adminPassword;

    public AdminService(ContributorRepository repository, String adminPassword) {
        this.repository    = repository;
        this.adminPassword = adminPassword;
    }

    @Override
    public void resetPassword(AdminResetPasswordCommand command) {
        boolean roleOk = ADMIN_ROLE.equals(command.getRole());
        boolean passOk = adminPassword != null && !adminPassword.isBlank()
            && adminPassword.equals(command.getPassword());
        if (!roleOk || !passOk) {
            throw new InvalidAdminCredentialsException();
        }

        Contributor c = repository.findByUsername(command.getUsername() == null
                ? null : command.getUsername().trim())
            .orElseThrow(() -> new ContributorNotFoundException(command.getUsername()));

        c.beginPasswordReset();
        repository.save(c);
    }
}
