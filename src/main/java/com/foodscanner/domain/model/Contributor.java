package com.foodscanner.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Слой: domain
 * Тип: Aggregate Root
 *
 * Участник каталогизации. В vNext получил аутентификацию:
 * username + passwordHash (BCrypt хранится снаружи, домен видит только строку),
 * счётчик неудачных входов, блокировку и окно восстановления пароля.
 *
 * Совместимость: фабрика create(nickname) сохранена для legacy-сценария
 * регистрации по нику. Для auth-пользователей nickname = username.
 *
 * Домен НЕ знает про BCrypt — хеширование/сверка в application/infrastructure.
 */
public final class Contributor {

    /** Бизнес-правила защиты от подбора и восстановления. */
    public static final int      MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION       = Duration.ofHours(24);
    public static final Duration RECOVERY_WINDOW     = Duration.ofMinutes(5);

    private final UUID    id;
    private       String  nickname;
    private       String  username;
    private       String  passwordHash;
    private       int     failedLoginAttempts;
    private       Instant lockedUntil;
    private       Instant resetPasswordUntil;
    private       int     completedCatalogCount;
    private final Instant createdAt;
    private       Instant updatedAt;

    private Contributor(UUID id, String nickname, String username, String passwordHash,
                        int failedLoginAttempts, Instant lockedUntil, Instant resetPasswordUntil,
                        int completedCatalogCount, Instant createdAt, Instant updatedAt) {
        this.id                    = id;
        this.nickname              = nickname;
        this.username              = username;
        this.passwordHash          = passwordHash;
        this.failedLoginAttempts   = failedLoginAttempts;
        this.lockedUntil           = lockedUntil;
        this.resetPasswordUntil    = resetPasswordUntil;
        this.completedCatalogCount = completedCatalogCount;
        this.createdAt             = createdAt;
        this.updatedAt             = updatedAt;
    }

    // ── Фабрики ──────────────────────────────────────────────

    /** Legacy: регистрация только по нику (без пароля). */
    public static Contributor create(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname must not be null or blank");
        }
        Instant now = Instant.now();
        return new Contributor(UUID.randomUUID(), nickname.trim(), null, null,
            0, null, null, 0, now, now);
    }

    /** vNext: создание с логином и BCrypt-хешем пароля. nickname = username. */
    public static Contributor createWithCredentials(String username, String passwordHash) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be null or blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("PasswordHash must not be null or blank");
        }
        Instant now = Instant.now();
        String u = username.trim();
        return new Contributor(UUID.randomUUID(), u, u, passwordHash, 0, null, null, 0, now, now);
    }

    /** Восстановление из хранилища. Только для ContributorRepositoryAdapter. */
    public static Contributor reconstitute(
            UUID id, String nickname, String username, String passwordHash,
            int failedLoginAttempts, Instant lockedUntil, Instant resetPasswordUntil,
            int completedCatalogCount, Instant createdAt, Instant updatedAt) {
        Objects.requireNonNull(id,        "id must not be null");
        Objects.requireNonNull(nickname,  "nickname must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        return new Contributor(id, nickname, username, passwordHash, failedLoginAttempts,
            lockedUntil, resetPasswordUntil, completedCatalogCount, createdAt, updatedAt);
    }

    // ── Аутентификация ───────────────────────────────────────

    public boolean hasPassword() { return passwordHash != null; }

    /** Заблокирован ли сейчас (подбор пароля). */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    /** Неудачная попытка входа: +1, при достижении порога — блок на 24ч. */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.lockedUntil = Instant.now().plus(LOCK_DURATION);
        }
        touch();
    }

    /** Успешный вход: сбросить счётчик и блокировку. */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        touch();
    }

    /** Админский сброс: пароль обнуляется, открывается окно восстановления 5 мин. */
    public void beginPasswordReset() {
        this.passwordHash = null;
        this.resetPasswordUntil = Instant.now().plus(RECOVERY_WINDOW);
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        touch();
    }

    /** В окне восстановления (пароль сброшен, время ещё не вышло). */
    public boolean isInRecovery() {
        return passwordHash == null && resetPasswordUntil != null
            && Instant.now().isBefore(resetPasswordUntil);
    }

    /** Окно восстановления истекло — аккаунт подлежит удалению. */
    public boolean isRecoveryExpired() {
        return passwordHash == null && resetPasswordUntil != null
            && !Instant.now().isBefore(resetPasswordUntil);
    }

    /** Установить новый пароль (после восстановления или смены). */
    public void setPassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("PasswordHash must not be null or blank");
        }
        this.passwordHash = newPasswordHash;
        this.resetPasswordUntil = null;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        touch();
    }

    // ── Бизнес-методы ────────────────────────────────────────

    public void incrementCompletedCatalogs() {
        this.completedCatalogCount++;
        touch();
    }

    private void touch() { this.updatedAt = Instant.now(); }

    // ── Геттеры ──────────────────────────────────────────────

    public UUID    getId()                    { return id; }
    public String  getNickname()              { return nickname; }
    public String  getUsername()              { return username; }
    public String  getPasswordHash()          { return passwordHash; }
    public int     getFailedLoginAttempts()   { return failedLoginAttempts; }
    public Instant getLockedUntil()           { return lockedUntil; }
    public Instant getResetPasswordUntil()    { return resetPasswordUntil; }
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
        return "Contributor{id=" + id + ", username='" + username + "'}";
    }
}
