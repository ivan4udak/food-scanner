package com.foodscanner.application.service;

import com.foodscanner.application.command.CompleteCatalogCommand;
import com.foodscanner.application.result.CompleteCatalogResult;
import com.foodscanner.application.usecase.CompleteCatalogUseCase;
import com.foodscanner.domain.exception.CatalogDraftNotFoundException;
import com.foodscanner.domain.model.CatalogDraft;
import com.foodscanner.domain.model.CatalogEntry;
import com.foodscanner.domain.model.Contributor;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.CatalogDraftRepository;
import com.foodscanner.domain.repository.CatalogEntryRepository;
import com.foodscanner.domain.repository.ContributorRepository;

/**
 * Слой: application
 * Тип: Use Case Implementation
 *
 * Транзакционная оркестрация (один transaction boundary):
 *   1. Найти черновик
 *   2. Проверить владельца
 *   3. policy.createEntry() → создаёт CatalogEntry + переводит draft в COMPLETED
 *   4. Сохранить CatalogEntry
 *   5. Сохранить черновик (теперь COMPLETED)
 *   6. Найти Contributor + incrementCompletedCatalogs() + сохранить
 *
 * @Transactional проставляется в Spring конфигурации, не здесь.
 * Это сохраняет use case чистым Java для unit-тестов без контекста.
 */
public class CompleteCatalogService implements CompleteCatalogUseCase {

    private final CatalogDraftRepository  draftRepository;
    private final CatalogEntryRepository  entryRepository;
    private final ContributorRepository   contributorRepository;
    private final CatalogCompletionPolicy policy;

    public CompleteCatalogService(
            CatalogDraftRepository draftRepository,
            CatalogEntryRepository entryRepository,
            ContributorRepository contributorRepository,
            CatalogCompletionPolicy policy) {
        this.draftRepository       = draftRepository;
        this.entryRepository       = entryRepository;
        this.contributorRepository = contributorRepository;
        this.policy                = policy;
    }

    @Override
    public CompleteCatalogResult execute(CompleteCatalogCommand command) {
        CatalogDraft draft = draftRepository.findById(command.getDraftId())
            .orElseThrow(() -> new CatalogDraftNotFoundException(command.getDraftId()));

        if (!draft.getContributorId().equals(command.getContributorId())) {
            throw new IllegalStateException(
                "Draft " + command.getDraftId() + " does not belong to contributor "
                + command.getContributorId());
        }

        // policy.createEntry() проверяет полноту, создаёт entry, переводит draft в COMPLETED
        CatalogEntry entry = policy.createEntry(draft);

        entryRepository.save(entry);
        draftRepository.save(draft);   // сохраняем COMPLETED статус

        Contributor contributor = contributorRepository.findById(command.getContributorId())
            .orElseThrow(() -> new IllegalStateException(
                "Contributor not found: " + command.getContributorId()));

        contributor.incrementCompletedCatalogs();
        contributorRepository.save(contributor);

        return new CompleteCatalogResult(
            entry.getId(),
            contributor.getCompletedCatalogCount());
    }
}
