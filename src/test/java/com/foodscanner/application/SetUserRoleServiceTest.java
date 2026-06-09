package com.foodscanner.application;

import com.foodscanner.application.service.SetUserRoleService;
import com.foodscanner.domain.exception.AccessDeniedException;
import com.foodscanner.domain.exception.ContributorNotFoundException;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.model.ContributorRole;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SetUserRoleServiceTest {

    private final ContributorRepository repo = mock(ContributorRepository.class);
    private final SetUserRoleService service = new SetUserRoleService(repo);

    @Test
    void superAdminCanChangeRole() {
        Contributor target = Contributor.createWithCredentials("bob", "h");
        when(repo.findById(target.getId())).thenReturn(Optional.of(target));

        ContributorRole result = service.execute(ContributorRole.SUPER_ADMIN, target.getId(), ContributorRole.ADMIN);

        assertThat(result).isEqualTo(ContributorRole.ADMIN);
        assertThat(target.getRole()).isEqualTo(ContributorRole.ADMIN);
        verify(repo).save(target);
    }

    @Test
    void adminCannotChangeRole() {
        assertThatThrownBy(() -> service.execute(ContributorRole.ADMIN, UUID.randomUUID(), ContributorRole.ADMIN))
            .isInstanceOf(AccessDeniedException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void userCannotChangeRole() {
        assertThatThrownBy(() -> service.execute(ContributorRole.USER, UUID.randomUUID(), ContributorRole.ADMIN))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void throwsWhenTargetMissing() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(ContributorRole.SUPER_ADMIN, id, ContributorRole.ADMIN))
            .isInstanceOf(ContributorNotFoundException.class);
    }
}
