package com.foodscanner.application;

import com.foodscanner.application.command.LoginCommand;
import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.port.TokenService;
import com.foodscanner.application.service.AuthService;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import com.foodscanner.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceAdminBootstrapTest {

    private static final PasswordHasher HASHER = new PasswordHasher() {
        public String hash(String raw) { return "H:" + raw; }
        public boolean matches(String raw, String hash) { return ("H:" + raw).equals(hash); }
    };
    private static final TokenService TOKENS = new TokenService() {
        public String issueAccessToken(UUID id, String username, String role) { return role + "." + id; }
        public AccessClaims verifyAccessToken(String token) { return null; }
    };

    private ContributorRepository repo;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repo = mock(ContributorRepository.class);
        RefreshTokenRepository refreshRepo = mock(RefreshTokenRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AuthService(repo, HASHER, TOKENS, refreshRepo, Duration.ofDays(30),
            Set.of("alice"), Set.of("boss"));
    }

    @Test
    void promotesConfiguredAdminOnLogin() {
        Contributor alice = Contributor.createWithCredentials("alice", "H:pw");
        when(repo.findByUsername("alice")).thenReturn(Optional.of(alice));

        service.login(new LoginCommand("alice", "pw"));

        assertThat(alice.isAdmin()).isTrue(); // авто-промоут до ADMIN
    }

    @Test
    void keepsNonAdminAsUser() {
        Contributor bob = Contributor.createWithCredentials("bob", "H:pw");
        when(repo.findByUsername("bob")).thenReturn(Optional.of(bob));

        service.login(new LoginCommand("bob", "pw"));

        assertThat(bob.isAdmin()).isFalse();
    }

    @Test
    void promotesConfiguredSuperAdminOnLogin() {
        Contributor boss = Contributor.createWithCredentials("boss", "H:pw");
        when(repo.findByUsername("boss")).thenReturn(Optional.of(boss));

        service.login(new LoginCommand("boss", "pw"));

        assertThat(boss.getRole()).isEqualTo(com.foodscanner.domain.model.ContributorRole.SUPER_ADMIN);
    }
}
