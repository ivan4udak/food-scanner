# Changelog

Версионирование (источник — файл `VERSION` в корне; CI показывает `v<VERSION>` в Telegram):
- **PATCH** — каждый фикс/мелкая доработка: `1.3.0 → 1.3.1 → … → 1.3.9 → 1.3.10 → 1.3.11 …`
- **MINOR** — новая крупная фича или изменение API: `→ 1.4.0`.
- Суффикс ветки в уведомлении: `test → v…t`, `main → v…s` (stable), `release → v…` (production).

Ветки/окружения: `test → staging`, `main → stable`, `release → production`.

---

## [Unreleased]
- —

## [1.7.0] — Серверные клиентские логи, телеметрия и публичный dashboard
- **Корреляция (API, аддитивно):** `CorrelationIdFilter` — заголовок `X-Correlation-Id`
  (читается из запроса либо генерируется), `requestId`, оба в MDC; ответ возвращает
  `X-Correlation-Id`. Клиент шлёт correlationId в каждом запросе.
- **Приём телеметрии (Bearer):** `POST /api/v1/client-logs/batch`, `POST /api/v1/client/session`,
  `POST /api/v1/client/activity`. Flyway V11: `client_logs` (+correlation/barcode/draft/entry/photo/api),
  `client_sessions`, `client_activity`, `server_events`. Серверная повторная маскировка секретов;
  успешные ping/health не сохраняются (heartbeat-шум). Retention: 30д обычные / 90д WARN+ERROR.
- **Публичная статистика (без авторизации):** `GET /api/v1/public/stats`,
  `GET /api/v1/public/leaderboard?period=all|today|week|month&limit`. Рейтинг по completedEntries
  (затем фото, затем сканы); скрытые участники исключены. Страница PWA `/stats`.
- **Кабинет:** `POST /api/v1/me/leaderboard-visibility` — opt-out из рейтинга. Flyway V12:
  `contributors.hidden_from_leaderboard`.
- **PWA:** отправка логов батчами (30с / 50 записей / WARN+ERROR / открытие диагностики) с backoff
  и очередью в localStorage; снимок сессии и активность; клиентский отсев ping/health.
- **CI/CD:** Telegram-уведомление о деплое теперь содержит Environment/Branch/Version/Commit +
  Service URL и API URL окружения (единая карта `service_url_for_env` в `deploy/scripts/lib.sh`),
  отдельные сообщения об успехе/провале/откате.
- Backend 191 тест (вкл. Testcontainers), фронт 50 тестов — зелёные.

## [1.6.1] — Кнопка настроек на всех экранах
- Шестерёнка ⚙ (переход на «О приложении» → Диагностика) добавлена в TopBar
  через проп `settings`; включена на экранах «Новый продукт» (черновик) и
  «Продукт» (просмотр) — раньше была только на экране сканирования.
  Экран сканирования переведён на тот же проп (без дублирования кнопки).

## [1.6.0] — Клиентское логирование и диагностика
- **Крупная фича:** клиентский логгер (`web/src/logging/logger.ts`) — уровни
  TRACE/DEBUG/INFO/WARN/ERROR, категории AUTH/API/NETWORK/SCAN/PHOTO/CATALOG/HEALTH/PWA/UI/SYSTEM,
  кольцевой буфер на 5000 записей в памяти + хвост из 1000 в localStorage (переживает перезагрузку).
- **Безопасность:** пароли, access/refresh-токены и заголовок `Authorization` никогда не пишутся в лог —
  маскируются (`Bearer ********`, ключи `password/token/...` → `********`), рекурсивно по объектам.
- **Покрытие событий:** axios-интерсепторы (запрос/ответ/длительность/размер/ретрай/refresh токена),
  переходы сети ConnectionMonitor (`ONLINE after 32s in OFFLINE`), auth (login/register/recover/logout),
  scan (сканер открыт/штрихкод/результат NEW|EXISTS), photo (старт/прогресс/успех/ошибка загрузки),
  catalog (черновик открыт/завершён, запись просмотрена), PWA (установка/standalone/баннеры).
- **Экран «О приложении» → Диагностика:** версия, браузер, backend URL, состояние связи, число записей,
  просмотр последних 100 логов, «Скопировать диагностику» (последние 500 + сведения об устройстве),
  «Скачать лог» → `food-scanner-log.txt` с полным дампом.
- **Тесты:** кольцевой буфер, фильтрация по уровню, persistence в localStorage, экспорт диагностики,
  маскировка секретов. Всего 44 фронт-теста зелёные.

## [1.5.1] — PWA: фикс перекрытия плашкой установки
- Нижняя плашка установки больше не перекрывает кнопку ручного ввода ШК
  (класс `body.install-banner-open` резервирует место снизу).

## [1.5.0] — Восстановление черновика (новый эндпоинт)
- **API (аддитивно):** `GET /api/v1/drafts/{draftId}` → состояние черновика владельца
  `{ draftId, barcode, status, photos[type,storageKey,capturedAt], uploadedCount, requiredCount, missingTypes, complete }`
  (по одному, последнему, фото на тип). DDD: `GetDraftUseCase`/`GetDraftService`, `DraftDetailsResult`,
  `DraftResponse`. 158 тестов зелёные.
- **PWA:** при входе в черновик слоты восстанавливаются с сервера (статус `done` + миниатюра по `storageKey`),
  счётчик и флаг «завершить» подтягиваются — раньше слоты были пустыми, хотя фото на сервере есть.
- `docs/API.md` обновлён.

## [1.4.0] — PWA: install-flow + фиксы загрузки фото
- **Крупная фича:** обязательный install-flow для iPhone (экран установки на «Домой» в Safari;
  в standalone не показывается; Android-кнопка установки через `beforeinstallprompt`;
  отложка на 24ч в localStorage + баннер-напоминание). `lib/platform.ts` (isStandalone/isIOS/isAndroid).
- Fix: retry загрузки фото досылает тот же файл из памяти (без повторного открытия камеры/галереи).
- Fix: карточка фото — подпись скрывается во время загрузки/ошибки; ошибка = красная карточка + иконка ⟳.
- Инфра: общий MinIO (`minio-shared`, bucket на окружение), окружение **stable** (ветка main),
  фикс прод-загрузки фото (коллизия алиаса `minio` на сети edge), версия в Telegram-уведомлениях.

## [1.3.0] — CI/CD для одного сервера (ветка feat/cicd)
- Backend (аддитивно): `spring-boot-starter-actuator` + `micrometer-registry-prometheus`,
  экспонирован `/actuator/prometheus` + health-пробы, метрики с тегом `env`. 156 тестов зелёные.
- Образы: runtime `Dockerfile` backend (temurin:21-jre, jar из CI, HEALTHCHECK) + web (multistage).
- GitHub Actions: `ci.yml` (dev/PR: тесты+сборка) и `deploy.yml`
  (push test/main/release → Tests → Maven Build → Docker Build → Push GHCR → Deploy SSH → Telegram).
- Окружения: `deploy/compose/*` — отдельные compose/volume/контейнеры/сети на staging(10690)/
  preprod(10790)/production(10890); секреты в `app.env` на сервере (не в репозитории).
- HTTPS: Caddy + Let's Encrypt (DuckDNS), маршрутизация по hostname, `/api`→backend.
- Production: **blue-green** без k8s (два цвета backend, переключение через Caddy reload),
  health-check новой версии и авто-rollback; история версий `releases.log` + ручной `rollback.sh`.
- Мониторинг: Prometheus + Grafana (provisioning + дашборд) + node_exporter + cAdvisor;
  Grafana за Caddy basic-auth. Логи: json-file ротация + logrotate.
- Скрипты/инфра: deploy/blue-green/rollback/health/notify/bootstrap, systemd (caddy+monitoring),
  env-примеры, runbook `deploy/README.md`.

## [1.2.0] — PWA-фронтенд (ветка feat/pwa)
- Новый устанавливаемый PWA-клиент в `web/` (React 18 + TS 5 + Vite 5), заменяет
  SwiftUI iOS-клиент; интегрирует существующий `/api/v1` **без изменений API**.
- Паритет функционала: Login/Register/Recover, Scan (BarcodeDetector + ZXing fallback),
  Draft + загрузка фото (getUserMedia/`input file` + сжатие `browser-image-compression`
  ≤1920/JPEG + EXIF capturedAt), Complete, Lookup, About + Diagnostics (/health).
- Стек: TanStack Query 5, React Router 6, Zustand (authStore/appStore), Axios
  (Bearer + авто-refresh access на 401 с очередью и повтором), Zod (валидация ответов).
- PWA: manifest + service worker (Workbox), offline-кэш каталога (`/entries`, NetworkFirst)
  и фото (`/photos`, CacheFirst). Устанавливается на iPhone через Safari → «На экран Домой».
- Инфраструктура: multistage `Dockerfile` (node→nginx), `nginx.conf` (SPA + прокси `/api`
  на `${BACKEND_URL}` + no-cache для SW), `docker-compose.yml` (порт 8081).
- TDD: 30 unit-тестов (vitest) — zod-схемы, нормализация ошибок, login-маппинг,
  refresh-retry, authStore, scan-gate, опции сжатия/EXIF. `tsc` + `vite build` зелёные.

## [1.1.10] — Блок 20: экран About + диагностический пакет
- Backend: новый публичный `GET /api/v1/health` (`HealthController` + `HealthResponse`),
  проверяет состояние хранилища через `PhotoStorage.isAvailable()` (MinIO `bucketExists`);
  `status` = OK / DEGRADED, поля `backend`/`storage`. `/ping` не изменён (heartbeat не утяжеляем).
- iOS: экран `AboutView` (Настройки → «О приложении»): версии iOS/приложения/сборки,
  Backend URL, состояние связи/Backend/MinIO, размер кэша, кнопка «Скопировать диагностику»
  (копирует весь пакет + Contributor ID/логин в буфер). `APIClient.health()` + `HealthResponse`.
- Версия приложения синхронизирована с git-тегами (для TestFlight):
  `MARKETING_VERSION 1.0 → 1.1.10`, `CURRENT_PROJECT_VERSION 1 → 20`.
- Тесты: `HealthControllerTest` (OK/DEGRADED) — всего **156 зелёных**.

## [1.1.9] — UX-фиксы и оптимизация
- Убрана «непонятная точка» острова: индикатор виден только при проблемах (degraded/offline)
  или во время сообщения; в норме (online/connecting) — ничего.
- Оптимизация: убрана постоянная анимация-пульс острова; offline-блокер без дорогого
  blur (сплошное затемнение) → меньше нагрузка на старте и на экране входа.
- Settings: Contributor ID копируется тапом (полный текст в буфер) + контекстное меню.
- Сканер: жёсткий кулдаун 4с между сканами — больше нет потока повторных запросов
  на один штрихкод при лагах; при возврате на экран скан доступен сразу.

## [1.1.8] — Блок 19: экран настроек
- `SettingsView` (через шестерёнку в сканере): адрес сервера (+ смена), статус сервера,
  логин, Contributor ID, версия и сборка приложения, «Очистить кэш» (картинки + офлайн-каталог),
  «Выйти из аккаунта».

## [1.1.7] — Блок 18: офлайн-кэш каталога (SwiftData)
- `CachedEntry` (@Model): barcode, name, типы/ключи фото, updatedAt; контейнер в App.
- `LookupView`: сначала показывает локальную запись, затем тихо обновляет с сервера и кэширует;
  при офлайне показывает кэш (фото — из дискового кэша Блока 9).
- `ImageStore.clear()/diskSize()` для управления кэшем.

## [1.1.6] — Блок 17: прогресс загрузки на iOS
- `APIClient.addPhoto` — прогресс отправки (URLSession upload + `UploadProgressDelegate`).
- `DraftViewModel`: последовательная очередь загрузок + состояния слота
  (queued / uploading N% / waitingServer / done / failed); общий счётчик «в очереди».
- Плитка: круговой индикатор с процентами, «сервер…» при ожидании ответа, «в очереди», галочка/ошибка.

## [1.1.5] — Остров в стиле Самоката (UX)
- Убрано кольцо; теперь статус-кружок справа от Dynamic Island (зелёный/жёлтый/красный/серый).
- При информировании (смена состояния связи) остров «расширяется» с сообщением ~1.8с,
  затем сворачивается в кружок (пружинная анимация). Адаптив под DI/notch/плоский верх.

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
