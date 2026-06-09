package com.foodscanner.infrastructure.security;

import com.foodscanner.application.port.TokenService;
import com.foodscanner.domain.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenService")
class JwtTokenServiceTest {

    private final JwtTokenService svc =
        new JwtTokenService("test-secret-test-secret-test-secret-32+chars", Duration.ofHours(24));

    @Test @DisplayName("issue → verify возвращает те же claims")
    void roundtrip() {
        UUID id = UUID.randomUUID();
        String jwt = svc.issueAccessToken(id, "alice", "ADMIN");
        TokenService.AccessClaims c = svc.verifyAccessToken(jwt);
        assertEquals(id, c.contributorId());
        assertEquals("alice", c.username());
        assertEquals("ADMIN", c.role());
    }

    @Test @DisplayName("повреждённый токен → InvalidTokenException")
    void tampered() {
        String jwt = svc.issueAccessToken(UUID.randomUUID(), "alice", "USER");
        assertThrows(InvalidTokenException.class, () -> svc.verifyAccessToken(jwt + "x"));
    }

    @Test @DisplayName("истёкший токен → InvalidTokenException")
    void expired() {
        JwtTokenService shortLived =
            new JwtTokenService("test-secret-test-secret-test-secret-32+chars", Duration.ofMillis(1));
        String jwt = shortLived.issueAccessToken(UUID.randomUUID(), "alice", "USER");
        try { Thread.sleep(20); } catch (InterruptedException ignored) {}
        assertThrows(InvalidTokenException.class, () -> shortLived.verifyAccessToken(jwt));
    }
}
