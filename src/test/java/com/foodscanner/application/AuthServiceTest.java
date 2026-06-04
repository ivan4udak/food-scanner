package com.foodscanner.application;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.port.TokenService;
import com.foodscanner.application.result.AuthSession;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.service.AuthService;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.exception.InvalidTokenException;
import com.foodscanner.domain.exception.RecoveryNotAllowedException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.model.RefreshToken;
import com.foodscanner.domain.repository.ContributorRepository;
import com.foodscanner.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AuthService (JWT + refresh)")
class AuthServiceTest {

    private ContributorRepository  repo;
    private RefreshTokenRepository refreshRepo;
    private AuthService service;

    private static final PasswordHasher FAKE = new PasswordHasher() {
        public String hash(String raw) { return "H:" + raw; }
        public boolean matches(String raw, String hash) { return hash != null && hash.equals("H:" + raw); }
    };
    private static final TokenService TOKENS = new TokenService() {
        public String issueAccessToken(UUID id, String username) { return "access." + id; }
        public AccessClaims verifyAccessToken(String token) { return null; }
    };

    @BeforeEach
    void setUp() {
        repo = mock(ContributorRepository.class);
        refreshRepo = mock(RefreshTokenRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repo.findByNickname(any())).thenReturn(Optional.empty());
        when(refreshRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AuthService(repo, FAKE, TOKENS, refreshRepo, Duration.ofDays(30));
    }

    private Contributor existing(String user, String pass) {
        return Contributor.createWithCredentials(user, "H:" + pass);
    }

    @Nested
    @DisplayName("login")
    class Login {
        @Test @DisplayName("OK + выдаёт токены при верном пароле")
        void okIssuesTokens() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            LoginResult r = service.login(new LoginCommand("alice", "secret"));

            assertEquals(LoginResult.Status.OK, r.getStatus());
            assertNotNull(r.getSession());
            assertNotNull(r.getSession().accessToken());
            assertNotNull(r.getSession().refreshToken());
            verify(refreshRepo).save(any(RefreshToken.class));
        }

        @Test @DisplayName("NOT_FOUND если пользователя нет")
        void notFound() {
            when(repo.findByUsername("ghost")).thenReturn(Optional.empty());
            assertEquals(LoginResult.Status.NOT_FOUND,
                service.login(new LoginCommand("ghost", "x")).getStatus());
        }

        @Test @DisplayName("INVALID при неверном пароле")
        void invalid() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));
            assertEquals(LoginResult.Status.INVALID_CREDENTIALS,
                service.login(new LoginCommand("alice", "wrong")).getStatus());
            assertEquals(1, c.getFailedLoginAttempts());
        }

        @Test @DisplayName("LOCKED после 5 неудач")
        void locked() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));
            LoginResult last = null;
            for (int i = 0; i < 5; i++) last = service.login(new LoginCommand("alice", "wrong"));
            assertEquals(LoginResult.Status.LOCKED, last.getStatus());
        }

        @Test @DisplayName("RECOVERY если пароль сброшен")
        void recovery() {
            Contributor c = existing("alice", "secret");
            c.beginPasswordReset();
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));
            assertEquals(LoginResult.Status.RECOVERY,
                service.login(new LoginCommand("alice", "any")).getStatus());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {
        @Test @DisplayName("создаёт и выдаёт токены")
        void createsWithTokens() {
            when(repo.findByUsername("bob")).thenReturn(Optional.empty());
            AuthSession s = service.register(new RegisterAccountCommand("bob", "pass"));
            assertNotNull(s.contributorId());
            assertNotNull(s.accessToken());
            assertNotNull(s.refreshToken());
        }

        @Test @DisplayName("409 если логин занят")
        void duplicate() {
            when(repo.findByUsername("bob")).thenReturn(Optional.of(existing("bob", "x")));
            assertThrows(ContributorAlreadyExistsException.class,
                () -> service.register(new RegisterAccountCommand("bob", "pass")));
        }

        @Test @DisplayName("мигрирует legacy без пароля (без дубля)")
        void claimsLegacy() {
            Contributor legacy = Contributor.create("oldie");
            when(repo.findByUsername("oldie")).thenReturn(Optional.empty());
            when(repo.findByNickname("oldie")).thenReturn(Optional.of(legacy));

            AuthSession s = service.register(new RegisterAccountCommand("oldie", "newpass"));

            assertEquals(legacy.getId(), s.contributorId());
            assertTrue(legacy.hasPassword());
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {
        @Test @DisplayName("валидный refresh → новые токены + ротация старого")
        void rotates() {
            UUID cid = UUID.randomUUID();
            RefreshToken stored = RefreshToken.issue(cid, "hash", Duration.ofDays(30));
            when(refreshRepo.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
            when(repo.findById(cid)).thenReturn(Optional.of(Contributor.createWithCredentials("u", "H:p")));

            AuthSession s = service.refresh("any-raw-token");

            assertNotNull(s.accessToken());
            assertNotNull(s.refreshToken());
            verify(refreshRepo).deleteById(stored.getId());        // старый удалён
            verify(refreshRepo).save(any(RefreshToken.class));     // новый сохранён
        }

        @Test @DisplayName("неизвестный refresh → 401")
        void unknown() {
            when(refreshRepo.findByTokenHash(anyString())).thenReturn(Optional.empty());
            assertThrows(InvalidTokenException.class, () -> service.refresh("nope"));
        }

        @Test @DisplayName("просроченный refresh → 401 + удаление")
        void expired() {
            RefreshToken stored = RefreshToken.issue(UUID.randomUUID(), "hash", Duration.ofMillis(-1));
            when(refreshRepo.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
            assertThrows(InvalidTokenException.class, () -> service.refresh("x"));
            verify(refreshRepo).deleteById(stored.getId());
        }

        @Test @DisplayName("пустой refresh → 401")
        void blank() {
            assertThrows(InvalidTokenException.class, () -> service.refresh(" "));
        }
    }
}
