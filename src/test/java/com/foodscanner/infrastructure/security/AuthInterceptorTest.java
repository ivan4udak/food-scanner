package com.foodscanner.infrastructure.security;

import com.foodscanner.application.port.TokenService;
import com.foodscanner.domain.exception.InvalidTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AuthInterceptor")
class AuthInterceptorTest {

    private final TokenService tokens = mock(TokenService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(tokens);

    @Test @DisplayName("валидный Bearer → кладёт contributorId в атрибут")
    void valid() {
        UUID id = UUID.randomUUID();
        when(tokens.verifyAccessToken(eq("good")))
            .thenReturn(new TokenService.AccessClaims(id, "alice", "USER"));
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good");

        assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), new Object()));
        assertEquals(id, req.getAttribute(AuthInterceptor.CONTRIBUTOR_ATTR));
    }

    @Test @DisplayName("без заголовка → 401 (InvalidTokenException)")
    void missing() {
        assertThrows(InvalidTokenException.class, () ->
            interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }

    @Test @DisplayName("чужая схема → 401")
    void wrongScheme() {
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Basic abc");
        assertThrows(InvalidTokenException.class, () ->
            interceptor.preHandle(req, new MockHttpServletResponse(), new Object()));
    }

    @Test @DisplayName("невалидный токен → 401")
    void invalid() {
        when(tokens.verifyAccessToken(any())).thenThrow(new InvalidTokenException("bad"));
        var req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer bad");
        assertThrows(InvalidTokenException.class, () ->
            interceptor.preHandle(req, new MockHttpServletResponse(), new Object()));
    }
}
