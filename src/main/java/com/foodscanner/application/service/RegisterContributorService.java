package com.foodscanner.application.service;

import com.foodscanner.application.command.RegisterContributorCommand;
import com.foodscanner.application.result.RegisterContributorResult;
import com.foodscanner.application.usecase.RegisterContributorUseCase;
import com.foodscanner.domain.exception.ContributorAlreadyExistsException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;

/**
 * Слой: application
 * Тип: Use Case Implementation
 *
 * Оркестрация:
 *   1. Проверить уникальность nickname
 *   2. Создать Contributor через доменную фабрику
 *   3. Сохранить
 *   4. Вернуть результат
 *
 * Зависимости: ContributorRepository (interface из domain).
 * Нет Spring аннотаций — тестируется без контекста.
 * @Transactional проставляется в Spring конфигурации (infrastructure).
 */
public class RegisterContributorService implements RegisterContributorUseCase {

    private final ContributorRepository contributorRepository;

    public RegisterContributorService(ContributorRepository contributorRepository) {
        this.contributorRepository = contributorRepository;
    }

    @Override
    public RegisterContributorResult execute(RegisterContributorCommand command) {
        String nickname = command.getNickname() != null
            ? command.getNickname().trim() : null;

        if (contributorRepository.existsByNickname(nickname)) {
            throw new ContributorAlreadyExistsException(nickname);
        }

        Contributor contributor = Contributor.create(nickname);
        contributorRepository.save(contributor);

        return new RegisterContributorResult(
            contributor.getId(),
            contributor.getNickname());
    }
}
