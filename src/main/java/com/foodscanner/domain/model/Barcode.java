package com.foodscanner.domain.model;

import java.util.Objects;

/**
 * Слой: domain
 * Тип: Value Object
 *
 * Зачем: штрихкод — не просто строка. Несёт правило валидации
 * и нормализации. Используется как ключ поиска в репозитории.
 *
 * Immutability: полная, все поля final.
 *
 * Расширение: добавить валидацию контрольной суммы EAN-13/EAN-8
 * при появлении требования к качеству входных данных.
 */
public final class Barcode {

    private final String value;

    public Barcode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Barcode must not be null or blank");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Barcode other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
