package com.foodscanner.application.service;

import com.foodscanner.application.usecase.SetUserRoleUseCase;
import com.foodscanner.domain.exception.AccessDeniedException;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.model.ContributorRole;
import com.foodscanner.domain.repository.ContributorRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Слой: application.
 * Смена роли пользователя. Разрешено только SUPER_ADMIN; ADMIN — нет (403).
 */
@Service
public class SetUserRoleService implements SetUserRoleUseCase {

    private final ContributorRepository repository;

    public SetUserRoleService(ContributorRepository repository) {
        this.repository = repository;
    }

    @Override
    public ContributorRole execute(ContributorRole callerRole, UUID targetId, ContributorRole newRole) {
        if (callerRole != ContributorRole.SUPER_ADMIN) {
            throw new AccessDeniedException("Менять роли может только супер-админ");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("Роль обязательна");
        }
        Contributor c = repository.findById(targetId)
            .orElseThrow(() -> new ContributorNotFoundException(targetId.toString()));
        c.assignRole(newRole);
        repository.save(c);
        return c.getRole();
    }
}
