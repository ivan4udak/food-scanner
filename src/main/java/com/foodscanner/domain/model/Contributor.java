package com.foodscanner.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Aggregate Root
 *
 * Зачем: участник каталогизации. Нужен в V1 для атрибуции данных
 * и подготовки к рейтингу (Этап 4).
 *
 * completedCatalogCount — доменный счётчик. Обновляется атомарно
 * при успешном завершении CatalogEntry через incrementCompletedCatalogs().
 * Это не вычисляемый агрегат по таблице — это намеренно денормализованный
 * счётчик для быстрого чтения лидерборда (Этап 4).
 *
 * Immutability: id и createdAt — final. Остальные поля изменяемы
 * только через методы агрегата.
 *
 * Расширение: rank, isActive — добавить в Этапе 4.
 */
public final class Contributor {

    private final UUID    id;
    private       String  nickname;
    private       int     completedCatalogCount;
    private final Instant createdAt;
    private       Instant updatedAt;

    private Contributor(
            UUID id,
            String nickname,
            int completedCatalogCount,
            Instant createdAt,
            Instant updatedAt) {
        this.id                   = id;
        this.nickname             = nickname;
        this.completedCatalogCount = completedCatalogCount;
        this.createdAt            = createdAt;
        this.updatedAt            = updatedAt;
    }

    // ──────────────────────────────────────────────
    // Фабричный метод
    // ──────────────────────────────────────────────

    public static Contributor create(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname must not be null or blank");
        }
        Instant now = Instant.now();
        return new Contributor(UUID.randomUUID(), nickname.trim(), 0, now, now);
    }

    /** Восстановление из хранилища. Только для ContributorRepositoryAdapter. */
    public static Contributor reconstitute(
            UUID id,
            String nickname,
            int completedCatalogCount,
            Instant createdAt,
            Instant updatedAt) {
        Objects.requireNonNull(id,        "id must not be null");
        Objects.requireNonNull(nickname,  "nickname must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        return new Contributor(id, nickname, completedCatalogCount, createdAt, updatedAt);
    }

    // ──────────────────────────────────────────────
    // Бизнес-методы
    // ──────────────────────────────────────────────

    /**
     * Увеличивает счётчик завершённых каталогов на 1.
     *
     * Вызывается атомарно при успешном создании CatalogEntry
     * внутри CompleteCatalogUseCase (один transaction boundary).
     *
     * Расширение: при появлении лидерборда (Этап 4) здесь же
     * можно пересчитывать rank или публиковать DomainEvent.
     */
    public void incrementCompletedCatalogs() {
        this.completedCatalogCount++;
        this.updatedAt = Instant.now();
    }

    // ──────────────────────────────────────────────
    // Геттеры
    // ──────────────────────────────────────────────

    public UUID    getId()                    { return id; }
    public String  getNickname()              { return nickname; }
    public int     getCompletedCatalogCount() { return completedCatalogCount; }
    public Instant getCreatedAt()             { return createdAt; }
    public Instant getUpdatedAt()             { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contributor other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Contributor{id=" + id + ", nickname='" + nickname
            + "', completedCatalogCount=" + completedCatalogCount + "}";
    }
}
