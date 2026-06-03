package com.foodscanner.application;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.command.RecoverPasswordCommand;
import com.foodscanner.application.command.RegisterAccountCommand;
import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.result.AccountResult;
import com.foodscanner.application.result.LoginResult;
import com.foodscanner.application.service.AuthService;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.exception.RecoveryNotAllowedException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AuthService")
class AuthServiceTest {

    private ContributorRepository repo;
    private AuthService service;

    /** Детерминированный фейк вместо BCrypt. */
    private static final PasswordHasher FAKE = new PasswordHasher() {
        public String hash(String raw) { return "H:" + raw; }
        public boolean matches(String raw, String hash) { return hash != null && hash.equals("H:" + raw); }
    };

    @BeforeEach
    void setUp() {
        repo = mock(ContributorRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AuthService(repo, FAKE);
    }

    private Contributor existing(String user, String pass) {
        return Contributor.createWithCredentials(user, "H:" + pass);
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("OK при верном пароле + сброс счётчика")
        void okOnValidPassword() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            LoginResult r = service.login(new LoginCommand("alice", "secret"));

            assertEquals(LoginResult.Status.OK, r.getStatus());
            assertEquals(c.getId(), r.getContributorId());
        }

        @Test
        @DisplayName("NOT_FOUND если пользователя нет")
        void notFound() {
            when(repo.findByUsername("ghost")).thenReturn(Optional.empty());
            assertEquals(LoginResult.Status.NOT_FOUND,
                service.login(new LoginCommand("ghost", "x")).getStatus());
        }

        @Test
        @DisplayName("INVALID_CREDENTIALS + инкремент при неверном пароле")
        void invalidIncrements() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            LoginResult r = service.login(new LoginCommand("alice", "wrong"));

            assertEquals(LoginResult.Status.INVALID_CREDENTIALS, r.getStatus());
            assertEquals(1, c.getFailedLoginAttempts());
            verify(repo).save(c);
        }

        @Test
        @DisplayName("LOCKED после 5 неудачных попыток")
        void lockedAfterFive() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            LoginResult last = null;
            for (int i = 0; i < 5; i++) {
                last = service.login(new LoginCommand("alice", "wrong"));
            }
            assertEquals(LoginResult.Status.LOCKED, last.getStatus());
            assertTrue(c.isLocked());
        }

        @Test
        @DisplayName("LOCKED если аккаунт уже заблокирован")
        void lockedWhenAlreadyLocked() {
            Contributor c = existing("alice", "secret");
            for (int i = 0; i < 5; i++) c.recordFailedLogin();
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            assertEquals(LoginResult.Status.LOCKED,
                service.login(new LoginCommand("alice", "secret")).getStatus());
        }

        @Test
        @DisplayName("RECOVERY если пароль сброшен админом")
        void recovery() {
            Contributor c = existing("alice", "secret");
            c.beginPasswordReset();
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            assertEquals(LoginResult.Status.RECOVERY,
                service.login(new LoginCommand("alice", "anything")).getStatus());
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("создаёт нового пользователя")
        void createsNew() {
            when(repo.findByUsername("bob")).thenReturn(Optional.empty());
            AccountResult r = service.register(new RegisterAccountCommand("bob", "pass"));
            assertNotNull(r.getContributorId());
            assertEquals("bob", r.getUsername());
            verify(repo).save(any(Contributor.class));
        }

        @Test
        @DisplayName("409 если логин занят")
        void duplicate() {
            when(repo.findByUsername("bob")).thenReturn(Optional.of(existing("bob", "x")));
            assertThrows(ContributorAlreadyExistsException.class,
                () -> service.register(new RegisterAccountCommand("bob", "pass")));
        }
    }

    @Nested
    @DisplayName("recoverPassword")
    class Recover {

        @Test
        @DisplayName("устанавливает новый пароль в окне восстановления")
        void setsNewPassword() {
            Contributor c = existing("alice", "secret");
            c.beginPasswordReset();
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));

            service.recoverPassword(new RecoverPasswordCommand("alice", "newpass"));

            assertTrue(c.hasPassword());
            assertFalse(c.isInRecovery());
            assertTrue(FAKE.matches("newpass", c.getPasswordHash()));
        }

        @Test
        @DisplayName("410 если не в окне восстановления")
        void notInRecovery() {
            Contributor c = existing("alice", "secret");
            when(repo.findByUsername("alice")).thenReturn(Optional.of(c));
            assertThrows(RecoveryNotAllowedException.class,
                () -> service.recoverPassword(new RecoverPasswordCommand("alice", "newpass")));
        }
    }
}
