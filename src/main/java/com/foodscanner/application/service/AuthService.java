package com.foodscanner.application.service;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.result.AccountResult;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.usecase.AuthUseCase;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.exception.RecoveryNotAllowedException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;

import java.util.Optional;

/**
 * Слой: application
 * Сценарии входа, создания аккаунта и восстановления пароля.
 *
 * Чистый Java: зависит только от доменного репозитория и порта PasswordHasher.
 * @Transactional проставляется в Spring-конфигурации (infrastructure).
 */
public class AuthService implements AuthUseCase {

    private final ContributorRepository repository;
    private final PasswordHasher        hasher;

    public AuthService(ContributorRepository repository, PasswordHasher hasher) {
        this.repository = repository;
        this.hasher     = hasher;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        Optional<Contributor> found = repository.findByUsername(norm(command.getUsername()));
        if (found.isEmpty()) {
            return LoginResult.notFound();
        }
        Contributor c = found.get();

        if (c.isLocked()) {
            return LoginResult.locked();
        }
        if (c.isInRecovery()) {
            return LoginResult.recovery(c.getUsername());
        }
        if (c.hasPassword() && hasher.matches(command.getPassword(), c.getPasswordHash())) {
            c.recordSuccessfulLogin();
            repository.save(c);
            return LoginResult.ok(c.getId(), c.getUsername());
        }

        // Неверный пароль — фиксируем попытку (возможна блокировка на 5-й).
        c.recordFailedLogin();
        repository.save(c);
        return c.isLocked() ? LoginResult.locked() : LoginResult.invalid();
    }

    @Override
    public AccountResult register(RegisterAccountCommand command) {
        String username = norm(command.getUsername());
        if (repository.findByUsername(username).isPresent()) {
            throw new ContributorAlreadyExistsException(username);
        }
        Contributor c = Contributor.createWithCredentials(username, hasher.hash(command.getPassword()));
        repository.save(c);
        return new AccountResult(c.getId(), c.getUsername());
    }

    @Override
    public AccountResult recoverPassword(RecoverPasswordCommand command) {
        Contributor c = repository.findByUsername(norm(command.getUsername()))
            .orElseThrow(() -> new ContributorNotFoundException(command.getUsername()));
        if (!c.isInRecovery()) {
            throw new RecoveryNotAllowedException();
        }
        c.setPassword(hasher.hash(command.getNewPassword()));
        repository.save(c);
        return new AccountResult(c.getId(), c.getUsername());
    }

    private static String norm(String s) {
        return s == null ? null : s.trim();
    }
}
