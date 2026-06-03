package com.foodscanner.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Barcode Value Object")
class BarcodeTest {

    @Nested
    @DisplayName("Создание")
    class Creation {

        @Test
        @DisplayName("Создаётся с валидным EAN-13")
        void shouldCreateWithValidEan13() {
            assertEquals("4607038310042", new Barcode("4607038310042").getValue());
        }

        @Test
        @DisplayName("Создаётся с валидным EAN-8")
        void shouldCreateWithValidEan8() {
            assertEquals("46070383", new Barcode("46070383").getValue());
        }

        @Test
        @DisplayName("Значение обрезается от пробелов")
        void shouldTrimWhitespace() {
            assertEquals("4607038310042", new Barcode("  4607038310042  ").getValue());
        }
    }

    @Nested
    @DisplayName("Валидация — отказ")
    class ValidationFailure {

        @Test
        @DisplayName("Отклоняет null")
        void shouldRejectNull() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> new Barcode(null));
            assertEquals("Barcode must not be null or blank", ex.getMessage());
        }

        @Test
        @DisplayName("Отклоняет пустую строку")
        void shouldRejectEmpty() {
            assertThrows(IllegalArgumentException.class, () -> new Barcode(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("Отклоняет blank строки")
        void shouldRejectBlank(String blank) {
            assertThrows(IllegalArgumentException.class, () -> new Barcode(blank));
        }
    }

    @Nested
    @DisplayName("Value semantics")
    class ValueSemantics {

        @Test
        @DisplayName("Равенство по значению")
        void shouldBeEqualByValue() {
            assertEquals(new Barcode("4607038310042"), new Barcode("4607038310042"));
        }

        @Test
        @DisplayName("Одинаковый hashCode для равных")
        void shouldHaveSameHashCode() {
            assertEquals(
                new Barcode("4607038310042").hashCode(),
                new Barcode("4607038310042").hashCode());
        }

        @Test
        @DisplayName("Разные значения — не равны")
        void shouldNotBeEqualWithDifferentValues() {
            assertNotEquals(new Barcode("4607038310042"), new Barcode("9999999999999"));
        }

        @Test
        @DisplayName("toString возвращает значение")
        void shouldReturnValueAsString() {
            assertEquals("4607038310042", new Barcode("4607038310042").toString());
        }
    }
}
