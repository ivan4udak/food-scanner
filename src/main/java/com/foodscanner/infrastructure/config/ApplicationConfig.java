package com.foodscanner.infrastructure.config;

import com.foodscanner.application.port.PasswordHasher;
import com.foodscanner.application.service.*;
import com.foodscanner.domain.policy.CatalogCompletionPolicy;
import com.foodscanner.domain.repository.*;
import com.foodscanner.infrastructure.security.BCryptPasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Слой: infrastructure
 *
 * Единственное место где Spring знает о зависимостях application слоя.
 * Use case сервисы — чистый Java, без @Service аннотаций.
 * @Transactional здесь, а не в application сервисах — это позволяет
 * тестировать сервисы без Spring контекста.
 */
@Configuration
@EnableTransactionManagement
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public CatalogCompletionPolicy catalogCompletionPolicy() {
        return new CatalogCompletionPolicy();
    }

    @Bean
    public PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    public AuthService authService(ContributorRepository contributorRepository,
                                   PasswordHasher passwordHasher) {
        return new AuthService(contributorRepository, passwordHasher);
    }

    @Bean
    public AdminService adminService(ContributorRepository contributorRepository,
                                     @Value("${admin.password:}") String adminPassword) {
        return new AdminService(contributorRepository, adminPassword);
    }

    @Bean
    public RegisterContributorService registerContributorService(
            ContributorRepository contributorRepository) {
        return new RegisterContributorService(contributorRepository);
    }

    @Bean
    public ScanBarcodeService scanBarcodeService(
            CatalogEntryRepository entryRepository,
            CatalogDraftRepository draftRepository) {
        return new ScanBarcodeService(entryRepository, draftRepository);
    }

    @Bean
    public AddDraftPhotoService addDraftPhotoService(
            CatalogDraftRepository draftRepository,
            CatalogCompletionPolicy policy) {
        return new AddDraftPhotoService(draftRepository, policy);
    }

    @Bean
    public CompleteCatalogService completeCatalogService(
            CatalogDraftRepository draftRepository,
            CatalogEntryRepository entryRepository,
            ContributorRepository contributorRepository,
            CatalogCompletionPolicy policy) {
        return new CompleteCatalogService(
            draftRepository, entryRepository, contributorRepository, policy);
    }

    @Bean
    public FindCatalogEntryByBarcodeService findCatalogEntryByBarcodeService(
            CatalogEntryRepository entryRepository) {
        return new FindCatalogEntryByBarcodeService(entryRepository);
    }
}
