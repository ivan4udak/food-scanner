package com.foodscanner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contributor Aggregate Root")
class ContributorTest {

    @Nested
    @DisplayName("Создание")
    class Creation {

        @Test
        @DisplayName("Создаётся с валидным nickname")
        void shouldCreateWithValidNickname() {
            Contributor c = Contributor.create("alice");
            assertNotNull(c.getId());
            assertEquals("alice", c.getNickname());
        }

        @Test
        @DisplayName("Nickname обрезается от пробелов")
        void shouldTrimNickname() {
            assertEquals("bob", Contributor.create("  bob  ").getNickname());
        }

        @Test
        @DisplayName("completedCatalogCount = 0 при создании")
        void shouldStartWithZeroCount() {
            assertEquals(0, Contributor.create("alice").getCompletedCatalogCount());
        }

        @Test
        @DisplayName("updatedAt равен createdAt при создании")
        void shouldHaveUpdatedAtEqualToCreatedAt() {
            Contributor c = Contributor.create("alice");
            assertEquals(c.getCreatedAt(), c.getUpdatedAt());
        }

        @Test
        @DisplayName("Уникальные UUID для разных контрибьюторов")
        void shouldGenerateUniqueIds() {
            assertNotEquals(Contributor.create("alice").getId(),
                            Contributor.create("bob").getId());
        }
    }

    @Nested
    @DisplayName("Валидация nickname")
    class NicknameValidation {

        @Test
        @DisplayName("Отклоняет null")
        void shouldRejectNull() {
            assertThrows(IllegalArgumentException.class, () -> Contributor.create(null));
        }

        @Test
        @DisplayName("Отклоняет пустую строку")
        void shouldRejectEmpty() {
            assertThrows(IllegalArgumentException.class, () -> Contributor.create(""));
        }

        @Test
        @DisplayName("Отклоняет blank строку")
        void shouldRejectBlank() {
            assertThrows(IllegalArgumentException.class, () -> Contributor.create("   "));
        }
    }

    @Nested
    @DisplayName("incrementCompletedCatalogs")
    class IncrementCompletedCatalogs {

        @Test
        @DisplayName("Увеличивает счётчик на 1")
        void shouldIncrementByOne() {
            Contributor c = Contributor.create("alice");
            c.incrementCompletedCatalogs();
            assertEquals(1, c.getCompletedCatalogCount());
        }

        @Test
        @DisplayName("Увеличивает счётчик многократно")
        void shouldIncrementMultipleTimes() {
            Contributor c = Contributor.create("alice");
            c.incrementCompletedCatalogs();
            c.incrementCompletedCatalogs();
            c.incrementCompletedCatalogs();
            assertEquals(3, c.getCompletedCatalogCount());
        }

        @Test
        @DisplayName("Обновляет updatedAt при инкременте")
        void shouldUpdateUpdatedAt() throws InterruptedException {
            Contributor c = Contributor.create("alice");
            Instant before = c.getUpdatedAt();
            Thread.sleep(2);
            c.incrementCompletedCatalogs();
            assertTrue(c.getUpdatedAt().isAfter(before));
        }
    }
}
