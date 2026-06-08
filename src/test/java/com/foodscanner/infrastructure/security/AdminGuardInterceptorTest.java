package com.foodscanner.infrastructure.security;

import com.foodscanner.domain.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminGuardInterceptorTest {

    private final AdminGuardInterceptor guard = new AdminGuardInterceptor();

    private boolean run(String role) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (role != null) req.setAttribute(AuthInterceptor.ROLE_ATTR, role);
        return guard.preHandle(req, new MockHttpServletResponse(), new Object());
    }

    @Test
    void adminPasses() {
        assertThat(run("ADMIN")).isTrue();
        assertThat(run("SUPER_ADMIN")).isTrue();
    }

    @Test
    void userDenied() {
        assertThatThrownBy(() -> run("USER")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingRoleDenied() {
        assertThatThrownBy(() -> run(null)).isInstanceOf(AccessDeniedException.class);
    }
}
