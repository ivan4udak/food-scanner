package com.foodscanner.application;

import com.foodscanner.application.command.AdminResetPasswordCommand;
import com.foodscanner.application.service.AdminService;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.exception.InvalidAdminCredentialsException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AdminService — сброс пароля")
class AdminServiceTest {

    private ContributorRepository repo;
    private AdminService service;

    @BeforeEach
    void setUp() {
        repo = mock(ContributorRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AdminService(repo, "ADMIN_SECRET");
    }

    @Test
    @DisplayName("сбрасывает пароль при верных role+password")
    void resetsOnValidAdmin() {
        Contributor c = Contributor.createWithCredentials("friend", "H:old");
        when(repo.findByUsername("friend")).thenReturn(Optional.of(c));

        service.resetPassword(new AdminResetPasswordCommand("volkov", "ADMIN_SECRET", "friend"));

        assertFalse(c.hasPassword());
        assertTrue(c.isInRecovery());
        verify(repo).save(c);
    }

    @Test
    @DisplayName("403 при неверной роли")
    void forbiddenWrongRole() {
        assertThrows(InvalidAdminCredentialsException.class,
            () -> service.resetPassword(new AdminResetPasswordCommand("hacker", "ADMIN_SECRET", "friend")));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("403 при неверном админ-пароле")
    void forbiddenWrongPassword() {
        assertThrows(InvalidAdminCredentialsException.class,
            () -> service.resetPassword(new AdminResetPasswordCommand("volkov", "wrong", "friend")));
    }

    @Test
    @DisplayName("404 если логин не найден")
    void notFound() {
        when(repo.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ContributorNotFoundException.class,
            () -> service.resetPassword(new AdminResetPasswordCommand("volkov", "ADMIN_SECRET", "ghost")));
    }
}
