package com.foodscanner.application.service;

import com.foodscanner.application.usecase.SetLeaderboardVisibilityUseCase;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.repository.ContributorRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Слой: application.
 * Переключение видимости участника в публичном рейтинге.
 */
@Service
public class SetLeaderboardVisibilityService implements SetLeaderboardVisibilityUseCase {

    private final ContributorRepository repository;

    public SetLeaderboardVisibilityService(ContributorRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean execute(UUID contributorId, boolean hidden) {
        Contributor contributor = repository.findById(contributorId)
            .orElseThrow(() -> new IllegalArgumentException("Contributor not found: " + contributorId));
        contributor.setHiddenFromLeaderboard(hidden);
        repository.save(contributor);
        return contributor.isHiddenFromLeaderboard();
    }
}
