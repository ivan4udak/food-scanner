# Changelog

Версионирование: семантическое, инкремент **PATCH** на каждый блок/правку (v1.1.1, v1.1.2, …).
- Каждой версии соответствует git-тег `vX.Y.Z`.
- Крупные блоки разрабатываются в отдельной ветке `feat/*`, затем мерж в `dev`.

Ветки: `main` (стабильная), `dev` (интеграция), `feat/*` (крупные блоки 13–20).

---

## [Unreleased]
- UX-нюансы iOS: остров в стиле Самоката (расширение + кружок справа), адрес сервера
  над клавиатурой, адаптация под размеры (5/6/7/X/15…).
- Блоки 17–20 (прогресс загрузки, офлайн-кэш каталога, экран настроек, About/TestFlight).

## [1.1.4] — Блок 16: SHA-256 дедупликация (ветка feat/dedup)
- Контент-адресное хранение: ключ = `photos/{sha256(full)}.jpg`. `PhotoStore`/`DeduplicatingPhotoStore`
  + порт `PhotoObjectRepository` + таблица `photo_objects` (V10). Дубликат не заливается повторно.
- Очистка (Блок 15) стала hash-aware: объект удаляется только если на него больше нет ссылок
  (draft_photos/catalog_entry_photos). Просмотр каталога по ШК остаётся публичным (любой клиент).
- Тесты: DeduplicatingPhotoStoreTest, обновлён PurgeStaleDraftsServiceTest — 154 зелёных.
- Проверено вживую: один файл в 2 черновика → 1 объект в photo_objects.

## [1.1.3] — Блок 15: очистка мусора (ветка feat/cleanup-job)
- `@Scheduled` (раз в час) `StaleDraftCleanupJob` → use-case `PurgeStaleDraftsService`:
  находит незавершённые черновики (OPEN/ABANDONED) старше 24ч, удаляет объекты MinIO
  (full + thumbnail, best-effort), затем сам draft (draft_photos — каскадно), логирует количество.
- Порты: `PhotoStorage.delete`, `CatalogDraftRepository.findStaleUnfinished/deleteById`.
- Конфиг `cleanup.draft-ttl-hours` / `cleanup.interval-ms`.
- Тесты: PurgeStaleDraftsServiceTest (удаление/best-effort/пусто) — 151 зелёный.

## [1.1.2] — Блок 14: защита API (Bearer) + разграничение скана (ветка feat/api-auth)
- Backend: `AuthInterceptor` + `WebConfig` — Bearer обязателен на `/api/v1/**`,
  кроме `auth/**` и `ping`. Контроллеры берут id пользователя из токена (request-атрибут),
  не доверяя телу. Владелец проверяется: загрузка фото/завершение чужого draft → 422.
  `HttpMessageNotReadableException` → 400.
- iOS: сканирование разрешено только при ONLINE; при OFFLINE/DEGRADED — пауза скана и
  статус (красный «нет подключения» / жёлтый «ожидание»), снимается при восстановлении.
- Тесты: AuthInterceptorTest (4), обновлены контракт-тесты (requestAttr, исключение WebConfig/AuthInterceptor) — 148 зелёных.
- Проверено вживую: без токена 401, ping 200, чужой draft 422, свой 200.

## [1.1.1] — Блок 13: JWT-авторизация (ветка feat/jwt)
- Backend: access-токен (JWT HS256, 24ч) + refresh-токен (30д, хэш в БД, ротация).
  `POST /api/v1/auth/refresh`; login/register/recover теперь возвращают пару токенов.
  Порт `TokenService` + `JwtTokenService` (jjwt); `RefreshToken` + репозиторий; миграция V9.
- iOS: токены в Keychain (`KeychainStore`), Bearer-заголовок, `refresh`,
  автообновление access при старте, logout при протухшем refresh.
- Тесты: JwtTokenServiceTest, AuthServiceTest (refresh/ротация), AuthControllerTest (refresh 200/401) — 144 зелёных.

## [1.1.0] — дизайн-ревизия индикатора соединения
- Убрана надпись-пилюля у Dynamic Island.
- Флюидное кольцо вокруг острова/камеры: зелёное (online), жёлтое (degraded), красное (offline).
- OFFLINE: редактируемый адрес сервера снизу; автоповтор подключения через 5с после ввода.
- Fix: `environmentObject` вынесен наружу оверлеев (краш OfflineBlocker без AppState).

## [1.0.0] — vNext (блоки 1–12)
- Авторизация логин/пароль (BCrypt), лок-аут 5→24ч (423), админ-сброс, восстановление, ping.
- Heartbeat + индикаторы соединения (ONLINE/DEGRADED/OFFLINE), Dynamic Island.
- Изображения: серверные thumbnail/full + сжатие на клиенте + двухуровневый кэш.
- Рамка сканера (4 угла), меню источника фото у касания, шестерёнка режима.
- iOS-вход (логин/создание/восстановление) + плавный pending-эффект.
- Багфикс: legacy-миграция при регистрации, понятная ошибка короткого пароля.

## [0.1.0] — базовая каталогизация
- Контрибьютор, скан штрихкода, черновик, завершение, просмотр записи.
- MinIO-хранилище фото (порт PhotoStorage), captured_at, docker-compose.
