# Food Scanner — журнал и итоговое резюме проделанной работы

> Полный, подробный отчёт по разработке: backend (Spring Boot) + frontend (SwiftUI iOS)
> + инфраструктура (PostgreSQL, MinIO, Docker). Документ описывает архитектуру,
> весь реализованный функционал, REST API, модель данных, запуск, тесты,
> известные ограничения и историю изменений по git-веткам.
>
> Связанный живой журнал клиента: `ios/DEVELOPMENT.md`.

---

## 1. Обзор и стек

**Food Scanner** — система каталогизации продуктов по штрихкоду: контрибьютор
сканирует штрихкод, фотографирует продукт (штрихкод, лицевая сторона, состав, КБЖУ
и опционально оборот/доп.), фото уходят в объектное хранилище, создаётся запись каталога.

| Слой | Технологии |
|------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Validation |
| БД | PostgreSQL 16, Flyway (миграции) |
| Объектное хранилище | MinIO (S3-совместимое), за портом `PhotoStorage` (S3 подключается без правок use-case) |
| Изображения (сервер) | Thumbnailator (resize/JPEG) |
| Пароли | BCrypt (`spring-security-crypto`, без полного Spring Security) |
| Frontend | SwiftUI (iOS 17+), Swift 6 toolchain, AVFoundation, PhotosUI, ImageIO, CryptoKit |
| Инфра | docker-compose (Postgres + MinIO) |

Архитектурный стиль backend — **DDD / Ports & Adapters**, проверяется ArchUnit-тестами.

---

## 2. Архитектура backend (DDD-слои)

```
api            HTTP-граница: контроллеры, DTO, маппер, GlobalExceptionHandler
application    use-case интерфейсы + сервисы (чистый Java), команды/результаты, ПОРТЫ
domain         агрегаты, value-objects, доменные сервисы/политики, доменные исключения, репозитории-порты
infrastructure реализации портов: JPA-адаптеры, MinIO, BCrypt, Thumbnailator, конфиг Spring, scheduled-задачи
```

**Правила (ArchitectureTest, обязательны):**
- `domain` не зависит от Spring, JPA, application, infrastructure.
- `application` не зависит от infrastructure и Spring Web.
- `api` не обращается к репозиториям напрямую.

**Ключевые порты (application):**
- `ContributorRepository`, `CatalogDraftRepository`, `CatalogEntryRepository` (domain).
- `PhotoStorage` — объектное хранилище (upload/download).
- `ImageProcessor` — ресайз/сжатие (full + thumbnail).
- `PasswordHasher` — хеширование паролей.

Реализации портов и проводка бинов — только в `infrastructure` (`ApplicationConfig`,
`StorageConfig`). Use-case сервисы создаются как обычные объекты (тестируются без Spring).

---

## 3. Git: ветки и организация

| Ветка | Назначение | HEAD |
|-------|-----------|------|
| `main` | базовая (каталогизация + фото MinIO + рамка сканера) | `0dcc828` |
| `dev` | актуальная разработка vNext (auth, соединение, изображения, UX, фиксы) | `23e26ec` |
| `backend-dev` | историческая ветка фичи фото-хранилища | `7595362` |
| `front-dev` | историческая ветка фронта (фото «под стеклом») | `8a7aea1` |

**`dev` (поверх `main`) — основные коммиты:**
```
23e26ec fix(auth): legacy-миграция при регистрации + понятная ошибка короткого пароля
9cc3e24 feat(ios): блоки 11-12 — меню источника у касания + шестерёнка режима фото
0076504 feat(ios): экран входа (логин/пароль/создание/восстановление) + плавный pending
e04577e feat(ios): блоки 5-6 — heartbeat + индикаторы соединения (Dynamic Island)
fb3791b feat(images): блоки 7-9 — thumbnail/full на сервере, сжатие на клиенте, кэш
1e9e512 feat(auth): авторизация по логину/паролю — блоки 1-4 (backend)
```
**`main` — база:**
```
0dcc828 fix(ios): рамка сканера — 4 угла с загибами
a8e4cc1 chore: docker-compose + MinIO-креды
e2fb526 merge(ios): фото под стеклом, multipart, captured_at из EXIF
7818aac merge(backend): MinIO + captured_at + 4 обязательных типа
5c56ffd добавка фронта
```

> Примечание: на одном из этапов из-за ручных git-операций (`reset`/`clean`)
> удалялись незакоммиченные файлы фронта — восстановлены из коммита `5c56ffd`
> (`git restore`). С тех пор всё фиксируется коммитами, артефакты (логи, xcuserdata)
> вынесены в `.gitignore`.

---

## 4. Реализованный функционал

### 4.1 Каталогизация (базовая, `CatalogController`)
- Регистрация контрибьютора по нику (legacy): `POST /contributors`.
- Скан штрихкода: `POST /scan` → `NEW` (новый, есть draftId) / `EXISTS` (уже в каталоге).
- Черновик: добавление фото `POST /drafts/{id}/photos`, завершение `POST /drafts/{id}/complete`.
- Просмотр записи: `GET /entries/{barcode}`.
- Политика завершения `CatalogCompletionPolicy`: **обязательны 4 типа** — `BARCODE`,
  `FRONT`, `INGREDIENTS`, `NUTRITION`; `BACK`/`EXTRA` опциональны. При завершении
  сохраняются ВСЕ загруженные фото; при нескольких фото одного типа берётся последнее.

### 4.2 Хранилище и обработка фото (Блоки 7–8)
- `POST /drafts/{id}/photos` — **multipart** (`file`, `contributorId`, `photoType`, `capturedAt?`).
- Сервер **не хранит оригинал**: генерирует
  **Full** (≤ 1920 по большей стороне, JPEG) и **Thumbnail** (~144 px, JPEG), кладёт обе в MinIO.
- Раздача: `GET /photos/{key}?size=thumb|full` (по умолчанию full).
- Клиент дополнительно сжимает фото перед отправкой (≤ 1920, JPEG, ориентир ≤ 800 КБ).
- `captured_at` (дата съёмки) берётся из EXIF (`DateTimeOriginal`) галерейного фото
  **до** сжатия; для камеры — текущее время; хранится в БД (nullable).
- **Факт-проверка:** загрузка 4000×3000 / 796 КБ → full 1920×1440 / 199 КБ + thumb 144×108 / 3.6 КБ.

### 4.3 Кэш изображений на клиенте (Блок 9)
- `ImageStore` (actor): **NSCache (память)** + **диск (Caches)**, ключ — SHA256.
  Порядок: память → диск → сеть; повторно не качает.
- `CachedImage` (SwiftUI): плитки каталога грузят **thumbnail**; тап → `PhotoViewer`
  открывает **full** (≤ FullHD) полноэкранно.

### 4.4 Авторизация (Блоки 1–4, `AuthController`/`AdminController`)
Модель `Contributor` расширена: `username`, `passwordHash` (BCrypt), `failedLoginAttempts`,
`lockedUntil`, `resetPasswordUntil` (миграция **V8**). Legacy `create(nickname)` сохранён,
для auth-пользователей `nickname = username`.

- `POST /auth/login` → `OK` (200) / `INVALID` (401, «Неверный логин или пароль») /
  `NOT_FOUND` (404) / `LOCKED` (423) / `RECOVERY` (200).
- `POST /auth/register` → создаёт аккаунт (201); занятый логин → 409.
- `POST /auth/recover` → новый пароль в окне восстановления (200) / вне окна (410).
- **Защита от подбора:** 5 неудач подряд → `lockedUntil = now + 24h`, ответ **423**;
  успешный вход обнуляет счётчик.
- **Админ-сброс:** `POST /admin/reset-password` `{role:"volkov", password, username}` —
  `role==volkov` и `password==ADMIN_PASSWORD` (из env, не хардкод). После сброса:
  `passwordHash=null`, окно восстановления **5 минут**, счётчик обнулён.
- **Окно восстановления:** в течение 5 мин юзер задаёт новый пароль; если не успел —
  `RecoveryCleanupJob` (`@Scheduled`, раз в минуту) удаляет аккаунт.
- **Legacy-миграция (багфикс):** при `register`, если есть аккаунт с таким ником
  без логина/пароля (`username=null`) — ему **присваивается** логин+пароль
  (`Contributor.claimCredentials`), а не создаётся дубль (иначе конфликт `uq nickname` → 500).

### 4.5 Heartbeat и индикаторы соединения (Блоки 5–6, iOS)
- `GET /ping` → `{status:"OK", timestamp}`.
- `ConnectionMonitor` (@MainActor): пинг каждые **5с** (async/await),
  состояния **ONLINE** (<10с) / **DEGRADED** (10–20с) / **OFFLINE** (>20с), старт/стоп по scenePhase.
- `ConnectionOverlay`:
  - подключение/реконнект → зелёный баннер «Подключение установлено» (1.2с);
  - degraded → жёлтая шапка с двумя вращающимися полосками;
  - **offline** → весь экран помутнён и некликабелен, красный треугольник
    «Нет соединения с сервером», снизу вращающийся серый спиннер + «подключение...» +
    капсула «Подключение к серверу»; загрузка фото заблокирована.
- **Dynamic Island**: `IslandStatus` — чёрная капсула-«остров» с цветным кружком
  состояния; детект DI по верхнему safe-area inset ≥ 59.
- Хаптики: success при реконнекте, warning при обрыве.

### 4.6 UI сканера (Блок 10)
- Минималистичная рамка: **четыре угла с загибами внутрь**, по центру, без анимаций,
  без бегущей линии (`ScannerCorners` Shape).
- Живое распознавание ШК камерой (AVFoundation): EAN-13/8, UPC-E, Code128/39/93,
  ITF-14, QR, DataMatrix, PDF417, Aztec; хаптик при распознавании.
  В симуляторе (нет камеры) — ручной ввод для отладки.

### 4.7 Выбор источника фото (Блоки 11–12, iOS)
- Режим хранится в `AppState.photoSource` — **только в памяти**, до конца сессии (не UserDefaults).
- **Блок 11:** пока режим не задан, тап по плитке открывает `Menu` **у места касания**
  («Сделать фото» / «Выбрать из галереи»); выбор запоминается на сессию.
- **Блок 12:** шестерёнка в тулбаре «Новый продукт» → «Режим загрузки фото» с галочкой (✓)
  у выбранного (Камера/Галерея) — режим по умолчанию для всех плиток.
- Когда режим задан, плитки открывают источник сразу.
- Карточка фото «под стеклом»: фото вписано в квадрат (scaledToFill + clip),
  поверх — иконка/название/подсказка, слегка прозрачные.

### 4.8 iOS-вход и плавность UX
- `LoginView`: шаги **Вход → Создать аккаунт** (подтверждение пароля при NOT_FOUND;
  валидация ≥4 символов) **→ Новый пароль** (RECOVERY).
- Ошибки сервера показываются с деталями (декод `ServerErrorResponse`).
- **Плавность без рывков** (`BusyController` + `busyOverlay`): при ожидании ответа
  экран замутняется (blur+dim **нарастают со временем**, `TimelineView(.animation)`),
  спиннер вращается **всё медленнее** (угол ~√t); следующий экран раскрывается
  только после ответа; смена Вход↔Сканер — плавный `.opacity`.

---

## 5. REST API (полный список)

Базовый префикс: `/api/v1`.

| Метод | Путь | Назначение | Коды |
|-------|------|-----------|------|
| POST | `/contributors` | регистрация по нику (legacy) | 201, 409 |
| POST | `/auth/login` | вход | 200(OK/RECOVERY), 401, 404, 423 |
| POST | `/auth/register` | создать аккаунт (или миграция legacy) | 201, 409, 400 |
| POST | `/auth/recover` | новый пароль в окне восстановления | 200, 410, 404 |
| POST | `/admin/reset-password` | админ-сброс пароля | 200, 403, 404 |
| GET  | `/ping` | heartbeat | 200 |
| POST | `/scan` | скан штрихкода | 200(NEW/EXISTS) |
| POST | `/drafts/{draftId}/photos` | загрузка фото (multipart) | 200, 404, 422, 400 |
| POST | `/drafts/{draftId}/complete` | завершить каталог | 201, 404, 422 |
| GET  | `/entries/{barcode}` | запись каталога | 200, 404 |
| GET  | `/photos/{key}?size=thumb\|full` | отдать фото | 200 |

**Маппинг исключений (`GlobalExceptionHandler`):** 409 (already exists), 404 (not found),
422 (not completable / illegal state), 400 (валидация / IllegalArgument), **423** (locked),
**403** (admin), **410** (recovery window), 405, 500.

---

## 6. Модель данных и миграции (Flyway, схема `food_catalog`)

```
V1 create_food_catalog_schema
V2 create_contributors
V3 create_catalog_drafts
V4 create_draft_photos
V5 create_catalog_entries
V6 create_catalog_entry_photos
V7 add_photo_captured_at          -- captured_at (nullable) в draft_photos и catalog_entry_photos
V8 add_contributor_auth           -- username(uq), password_hash, failed_login_attempts,
                                     locked_until, reset_password_until
```

**Агрегаты:** `Contributor`, `CatalogDraft` (→ `DraftPhoto`), `CatalogEntry` (→ `CatalogEntryPhoto`).
`PhotoType`: `BARCODE, FRONT, BACK, INGREDIENTS, NUTRITION, EXTRA`.

---

## 7. Конфигурация и переменные окружения

`application.yml` (значения по умолчанию подогнаны под `docker-compose.yml`):

| Переменная | Назначение | Default |
|-----------|-----------|---------|
| `DB_URL/DB_USER/DB_PASSWORD` | PostgreSQL | localhost:5432 / foodscanner / foodscanner |
| `STORAGE_PROVIDER` | провайдер хранилища | minio |
| `MINIO_ENDPOINT` | адрес MinIO | http://localhost:9000 |
| `MINIO_ACCESS_KEY/SECRET_KEY` | креды MinIO | foodscanner / foodscanner123 |
| `MINIO_BUCKET` | бакет (создаётся при старте) | food-images |
| `ADMIN_PASSWORD` | пароль для `/admin/reset-password` | (пусто → админ выключен) |
| `MAX_FILE_SIZE/MAX_REQUEST_SIZE` | лимиты multipart | 25MB |

iOS: адрес backend настраивается на экране входа (по умолчанию `http://localhost:8080`).

---

## 8. Запуск

```bash
# 1) Инфраструктура
docker compose up -d                 # Postgres :5432, MinIO :9000 (консоль :9001)

# 2) Backend (с админ-паролем для сброса)
ADMIN_PASSWORD=123123 mvn spring-boot:run
#   Flyway применит миграции, MinIO-бакет создастся автоматически.

# 3) iOS
open ios/FoodScanner.xcodeproj       # Xcode 16+, выбрать iPhone-симулятор, ⌘R
```

> Симулятор видит хост как `localhost`. Для реального устройства задать IP в настройках входа.
> На реальном iPhone доступны камера, хаптики и зона Dynamic Island.

---

## 9. Тесты и проверенные сценарии

- **138 unit/contract-тестов — зелёные** (`mvn test`): domain (Barcode, Contributor,
  CatalogDraft, policy), application (Auth, Admin, AddDraftPhoto, CompleteCatalog,
  ScanBarcode, FindEntry), api (CatalogController, AuthController — WebMvcTest),
  infrastructure (ThumbnailatorImageProcessor), плюс ArchitectureTest (правила слоёв).
- **Проверено вживую (curl + симулятор) против Postgres+MinIO:**
  - register→201, login OK→200, неверный пароль→401, **5-я попытка→423**,
    admin reset→RECOVERY→recover→login новым паролем→200, неверный админ-пароль→403;
  - **legacy по нику**: login→404, register→201 (миграция), login→200;
  - загрузка фото → full+thumb в MinIO, `?size=thumb` отдаёт лёгкое превью;
  - индикатор соединения: OFFLINE-блокер при недоступном сервере, возврат в ONLINE.

> Интеграционные тесты репозиториев (`*IT`, Testcontainers) требуют Docker и запускаются
> отдельно (Failsafe), surefire их не гоняет.

---

## 10. Багфиксы

1. **«Не удалось создать аккаунт» для старого юзера без пароля** — конфликт
   `uq_contributors_nickname` (register создавал дубль) → 500. Исправлено
   legacy-миграцией (`claimCredentials`).
2. **«Не удалось создать аккаунт» при коротком пароле** — register требует 4..100 → 400,
   клиент показывал общий текст. Исправлено: декод `ServerErrorResponse` (показ деталей) +
   клиентская валидация (≥4, кнопка заблокирована).
3. Стрэй-дубли `* 2.java`/`model 2` в `target/` ломали surefire — чистятся `mvn clean`.

---

## 11. Структура iOS (`ios/FoodScanner`, 22 .swift)

```
App/            FoodScannerApp (entry), RootView, ScanFlowView, Route, environment+overlays
DesignSystem/   Theme, Components, CachedImage, BusyOverlay, ConnectionOverlay
Networking/     APIClient, Models, ImageStore (кэш), ImageCompressor
Session/        AppState (профиль/URL/photoSource), ConnectionMonitor(+Haptics, DeviceInfo), PhotoSlot
Features/
  Onboarding/   LoginView (+ ServerSettingsView)
  Scan/         ScanView (рамка-уголки), BarcodeScanner (AVFoundation)
  Draft/        DraftView (плитки/меню/шестерёнка), DraftViewModel, CameraCapture
  Result/       ExistsView, CompletedView
  Lookup/       LookupView (+ PhotoViewer)
```

---

## 12. Известные ограничения и дальнейший план

**Сделано:** vNext блоки 1–12 закрыты; каталогизация, фото-пайплайн, авторизация,
соединение, кэш, UX — на ветке `dev`.

**Осталось / опционально:**
- Слить `dev` → `main`.
- Хаптики на больше сценариев; Live Activity (ActivityKit) для «настоящего» Dynamic Island.
- OCR состава/КБЖУ (будущий этап).
- Множественные фото одного типа (сейчас домен хранит 1 на тип — нужен `List`).
- S3-реализация `PhotoStorage` рядом с MinIO (порт уже готов).
- Защита админ-эндпоинта на сетевом уровне; rate-limiting.

---

_Документ поддерживается вручную; детальный клиентский журнал — `ios/DEVELOPMENT.md`._
