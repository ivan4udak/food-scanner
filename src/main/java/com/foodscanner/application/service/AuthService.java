package com.foodscanner.application.service;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.port.TokenService;
import com.foodscanner.application.result.AuthSession;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.usecase.AuthUseCase;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.exception.InvalidTokenException;
import com.foodscanner.domain.exception.RecoveryNotAllowedException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.model.RefreshToken;
import com.foodscanner.domain.repository.ContributorRepository;
import com.foodscanner.domain.repository.RefreshTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Слой: application
 * Вход, создание аккаунта, восстановление пароля и обновление токена.
 *
 * При успехе выпускается пара токенов: access (JWT, через TokenService) +
 * refresh (случайный, в БД хранится только SHA-256-хэш). Refresh ротируется
 * при каждом обновлении.
 */
public class AuthService implements AuthUseCase {

    private static final SecureRandom RNG = new SecureRandom();

    private final ContributorRepository   repository;
    private final PasswordHasher          hasher;
    private final TokenService            tokenService;
    private final RefreshTokenRepository  refreshTokens;
    private final Duration                refreshTtl;

    public AuthService(ContributorRepository repository, PasswordHasher hasher,
                       TokenService tokenService, RefreshTokenRepository refreshTokens,
                       Duration refreshTtl) {
        this.repository    = repository;
        this.hasher        = hasher;
        this.tokenService  = tokenService;
        this.refreshTokens = refreshTokens;
        this.refreshTtl    = refreshTtl;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        Optional<Contributor> found = repository.findByUsername(norm(command.getUsername()));
        if (found.isEmpty()) return LoginResult.notFound();
        Contributor c = found.get();

        if (c.isLocked())     return LoginResult.locked();
        if (c.isInRecovery()) return LoginResult.recovery(c.getUsername());

        if (c.hasPassword() && hasher.matches(command.getPassword(), c.getPasswordHash())) {
            c.recordSuccessfulLogin();
            repository.save(c);
            return LoginResult.ok(issueSession(c));
        }

        c.recordFailedLogin();
        repository.save(c);
        return c.isLocked() ? LoginResult.locked() : LoginResult.invalid();
    }

    @Override
    public AuthSession register(RegisterAccountCommand command) {
        String username = norm(command.getUsername());
        if (repository.findByUsername(username).isPresent()) {
            throw new ContributorAlreadyExistsException(username);
        }
        // Миграция legacy-аккаунта (ник без логина/пароля) вместо дубля.
        Optional<Contributor> legacy = repository.findByNickname(username);
        if (legacy.isPresent() && legacy.get().isLegacyWithoutCredentials()) {
            Contributor c = legacy.get();
            c.claimCredentials(username, hasher.hash(command.getPassword()));
            repository.save(c);
            return issueSession(c);
        }
        Contributor c = Contributor.createWithCredentials(username, hasher.hash(command.getPassword()));
        repository.save(c);
        return issueSession(c);
    }

    @Override
    public AuthSession recoverPassword(RecoverPasswordCommand command) {
        Contributor c = repository.findByUsername(norm(command.getUsername()))
            .orElseThrow(() -> new ContributorNotFoundException(command.getUsername()));
        if (!c.isInRecovery()) throw new RecoveryNotAllowedException();
        c.setPassword(hasher.hash(command.getNewPassword()));
        repository.save(c);
        return issueSession(c);
    }

    @Override
    public AuthSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token missing");
        }
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(refreshToken))
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));
        if (stored.isExpired()) {
            refreshTokens.deleteById(stored.getId());
            throw new InvalidTokenException("Refresh token expired");
        }
        Contributor c = repository.findById(stored.getContributorId())
            .orElseThrow(() -> new InvalidTokenException("Contributor gone"));
        refreshTokens.deleteById(stored.getId());   // ротация
        return issueSession(c);
    }

    // ── helpers ───────────────────────────────────────────────

    private AuthSession issueSession(Contributor c) {
        String access     = tokenService.issueAccessToken(c.getId(), c.getUsername());
        String refreshRaw  = randomToken();
        refreshTokens.save(RefreshToken.issue(c.getId(), sha256(refreshRaw), refreshTtl));
        return new AuthSession(c.getId(), c.getUsername(), access, refreshRaw);
    }

    private static String randomToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String norm(String s) { return s == null ? null : s.trim(); }
}
