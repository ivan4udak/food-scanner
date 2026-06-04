package com.foodscanner.application.result;

/**
 * Слой: application
 * Итог попытки входа. Контроллер мапит status → HTTP-код.
 */
public final class LoginResult {

    public enum Status {
        OK,                  // 200 — вход выполнен (есть session с токенами)
        INVALID_CREDENTIALS, // 401 — неверный логин или пароль
        NOT_FOUND,           // 404 — пользователя нет (клиент предложит создать)
        LOCKED,              // 423 — заблокирован
        RECOVERY             // 200 — режим восстановления (нужен новый пароль)
    }

    private final Status      status;
    private final AuthSession session;   // только для OK
    private final String      username;  // для RECOVERY

    private LoginResult(Status status, AuthSession session, String username) {
        this.status   = status;
        this.session  = session;
        this.username = username;
    }

    public static LoginResult ok(AuthSession session)   { return new LoginResult(Status.OK, session, session.username()); }
    public static LoginResult recovery(String username) { return new LoginResult(Status.RECOVERY, null, username); }
    public static LoginResult invalid()                 { return new LoginResult(Status.INVALID_CREDENTIALS, null, null); }
    public static LoginResult notFound()                { return new LoginResult(Status.NOT_FOUND, null, null); }
    public static LoginResult locked()                  { return new LoginResult(Status.LOCKED, null, null); }

    public Status      getStatus()   { return status; }
    public AuthSession getSession()  { return session; }
    public String      getUsername() { return username; }
}
