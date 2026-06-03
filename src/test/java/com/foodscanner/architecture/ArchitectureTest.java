package com.foodscanner.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Слой: test / architecture
 *
 * Проверяет архитектурные ограничения из ARCHITECTURE_RULES.md.
 * Нарушение → тест упадёт с точным указанием класса и причины.
 * Запускается как обычный unit-тест — без Spring контекста.
 */
@DisplayName("Architecture Rules")
class ArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void loadClasses() {
        classes = new ClassFileImporter()
            .importPackages("com.foodscanner");
    }

    @Test
    @DisplayName("Domain не зависит от Spring")
    void domainShouldNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain не зависит от JPA")
    void domainShouldNotDependOnJpa() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application не зависит от Infrastructure")
    void applicationShouldNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application не зависит от Spring Web")
    void applicationShouldNotDependOnSpringWeb() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework.web..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain не зависит от Application")
    void domainShouldNotDependOnApplication() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Controllers не обращаются к Repository напрямую")
    void controllersShouldNotAccessRepositories() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..");

        rule.check(classes);
    }
}
