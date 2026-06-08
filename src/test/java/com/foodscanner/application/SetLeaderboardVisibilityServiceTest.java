package com.foodscanner.application;

import com.foodscanner.application.service.SetLeaderboardVisibilityService;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SetLeaderboardVisibilityServiceTest {

    private final ContributorRepository repository = mock(ContributorRepository.class);
    private final SetLeaderboardVisibilityService service = new SetLeaderboardVisibilityService(repository);

    @Test
    void hidesContributorAndPersists() {
        Contributor c = Contributor.createWithCredentials("alice", "hash");
        when(repository.findById(c.getId())).thenReturn(Optional.of(c));

        boolean hidden = service.execute(c.getId(), true);

        assertThat(hidden).isTrue();
        assertThat(c.isHiddenFromLeaderboard()).isTrue();
        verify(repository).save(c);
    }

    @Test
    void throwsWhenContributorMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(id, true))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
