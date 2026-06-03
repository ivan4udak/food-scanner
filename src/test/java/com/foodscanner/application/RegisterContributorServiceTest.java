package com.foodscanner.application;

import com.foodscanner.application.command.RegisterContributorCommand;
import com.foodscanner.application.result.RegisterContributorResult;
import com.foodscanner.application.service.RegisterContributorService;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Слой: application
 * TDD тест для RegisterContributorService.
 * Репозиторий мокируется — тестируем оркестрацию, не инфраструктуру.
 */
@DisplayName("RegisterContributorUseCase")
class RegisterContributorServiceTest {

    private ContributorRepository repository;
    private RegisterContributorService service;

    @BeforeEach
    void setUp() {
        repository = mock(ContributorRepository.class);
        service    = new RegisterContributorService(repository);
    }

    @Nested
    @DisplayName("Успешная регистрация")
    class Success {

        @Test
        @DisplayName("Регистрирует нового контрибьютора и возвращает результат")
        void shouldRegisterNewContributor() {
            when(repository.existsByNickname("alice")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RegisterContributorResult result =
                service.execute(new RegisterContributorCommand("alice"));

            assertNotNull(result.getContributorId());
            assertEquals("alice", result.getNickname());
        }

        @Test
        @DisplayName("Сохраняет контрибьютора в репозитории")
        void shouldSaveContributor() {
            when(repository.existsByNickname("alice")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new RegisterContributorCommand("alice"));

            verify(repository).save(any(Contributor.class));
        }

        @Test
        @DisplayName("Nickname обрезается от пробелов")
        void shouldTrimNickname() {
            when(repository.existsByNickname("alice")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RegisterContributorResult result =
                service.execute(new RegisterContributorCommand("  alice  "));

            assertEquals("alice", result.getNickname());
        }
    }

    @Nested
    @DisplayName("Защита от дублей")
    class DuplicateGuard {

        @Test
        @DisplayName("Бросает исключение если nickname уже занят")
        void shouldThrowWhenNicknameExists() {
            when(repository.existsByNickname("alice")).thenReturn(true);

            assertThrows(ContributorAlreadyExistsException.class,
                () -> service.execute(new RegisterContributorCommand("alice")));
        }

        @Test
        @DisplayName("Не сохраняет если nickname уже занят")
        void shouldNotSaveWhenNicknameExists() {
            when(repository.existsByNickname("alice")).thenReturn(true);

            try { service.execute(new RegisterContributorCommand("alice")); }
            catch (ContributorAlreadyExistsException ignored) {}

            verify(repository, never()).save(any());
        }
    }
}
