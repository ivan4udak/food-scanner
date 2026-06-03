# Food Scanner iOS — журнал разработки

> Живой документ. Обновляется при каждой доработке, чтобы наращивать
> функционал поверх существующего, а не переписывать с нуля.

## Контекст
- Backend: Spring Boot (`/api/v1`, порт 8080), DDD-слои, Postgres + Flyway.
- Frontend: нативный SwiftUI (`ios/FoodScanner`), Xcode 16+,
  file-system-synchronized группа (новые `.swift` подхватываются сами).

## Архитектура фронта
```
App/          точка входа + роутинг (NavigationStack, enum Route)
DesignSystem/ Theme (палитра/метрики), Components (кнопки, кольцо, баннеры)
Networking/   APIClient (async/await) + Codable-модели DTO
Session/      AppState (профиль+URL в UserDefaults), PhotoSlot
Features/     Onboarding · Scan · Draft · Result · Lookup
```

## Реализовано
- [x] Регистрация контрибьютора (`POST /contributors`), профиль в UserDefaults.
- [x] Настройка base URL сервера (экран входа → сеть).
- [x] **Скан штрихкода живой камерой** (AVFoundation, `BarcodeScanner.swift`):
      EAN-13/8, UPC-E, Code128/39/93, ITF-14, QR, DataMatrix, PDF417, Aztec.
      Камера открывается сразу, код распознаётся автоматически (без ручного ввода).
      Fallback с ручным вводом — только в симуляторе (нет камеры).
- [x] Ветвление скана: `NEW` (зелёный) → черновик, `EXISTS` (красный) → запись.
- [x] Сбор фото черновика, прогресс-кольцо, `POST /drafts/{id}/photos`.
- [x] Завершение каталога (`POST /drafts/{id}/complete`), экран успеха.
- [x] Просмотр записи (`GET /entries/{barcode}`).

## Итерация 2 (в работе) — UX камеры и фото
- [x] Убрать зелёную бегущую линию у рамки сканера.
- [x] Рамка скана: нейтральный серый прямоугольник, скруглённые края,
      затемнение вокруг (cutout), минимализм.
- [x] На экране черновика выбор источника: «Сделать фото» / «Из галереи».
      Выбор делается один раз — далее тап по слоту сразу открывает источник.
- [x] «Сделать фото»: системная камера, кадр в память, НЕ сохраняется на устройство.
- [x] «Из галереи»: PhotosPicker, кадр в память.
- [x] Загрузка на сервер сразу после выбора кадра (POST metadata storageKey).
- [x] Обязательные = ШК, Лицевая (FRONT), Состав (INGREDIENTS), КБЖУ (NUTRITION) — выделены.
      Опциональные = Оборот (BACK), Дополнительно (EXTRA).
- [x] Кнопка «Завершить» открывается при наборе 4 обязательных.
- [x] Карточки фото: изображение корректно вписано (aspect-fill + clip).
- [x] Backend: `CatalogCompletionPolicy.REQUIRED_TYPES` → 4 типа; createEntry
      сохраняет ВСЕ загруженные фото (вкл. опциональные). Тесты обновлены.

## Известные ограничения / TODO (бэкенд, будущие этапы)
- ⚠️ **Нет binary-upload эндпоинта.** Сейчас `POST /photos` принимает только
  строку `storageKey`. Реальные байты изображения никуда не загружаются —
  фронт шлёт сгенерированный ключ. Для настоящей загрузки нужен Stage-2:
  multipart/presigned-URL эндпоинт + объектное хранилище (S3/MinIO) +
  схема хранения. До этого «загрузка на сервер» = регистрация метаданных фото.
- ⚠️ Несколько фото одного типа: домен хранит `Map<PhotoType,String>` (1 на тип).
  Для множественных EXTRA нужен переход на `List` в `CatalogEntry`/`CatalogDraft`.
- OCR по INGREDIENTS/NUTRITION (Stage 2 по комментариям в домене).

## Заметки по реализации
- Камера в память: `UIImagePickerController(sourceType: .camera)` — кадр
  отдаётся как `UIImage` в delegate, в фотоплёнку НЕ пишется (не вызываем
  `UIImageWriteToSavedPhotosAlbum`).
- Прогресс-кольцо считает только обязательные: `uploadedCount/requiredCount`
  от бэкенда (= число загруженных обязательных типов).

---

## vNext — журнал (ветка dev)

### Сделано: реальная загрузка фото (закрыло прежнее ограничение)
- `POST /drafts/{id}/photos` теперь multipart: байты заливаются в MinIO,
  в БД — storage_key; `GET /photos/{key}` отдаёт фото. captured_at из EXIF.

### Блок 10 ✅ — рамка сканера
- Убраны затемнение/линия. `ScannerCorners` (Shape): 4 угла с загибами, без анимации.

### Блоки 1-4 ✅ — авторизация (backend, ветка dev)
- `Contributor` расширен: username, passwordHash (BCrypt), failedLoginAttempts,
  lockedUntil, resetPasswordUntil. Legacy `create(nickname)` сохранён, nickname=username.
- Миграция **V8** (nullable username/password_hash + uq_username).
- Порт `PasswordHasher` (application) + `BCryptPasswordHasher` (infra, spring-security-crypto).
- `AuthService`: login (OK/INVALID/NOT_FOUND/LOCKED/RECOVERY), register, recoverPassword.
- `AdminService`: reset-password (role==volkov + ADMIN_PASSWORD из env, не хардкод).
- Лок-аут: 5 неудач → `lockedUntil = now+24h`, HTTP 423.
- Восстановление: админ-сброс → passwordHash=null + окно 5 мин; `RecoveryCleanupJob`
  (@Scheduled, раз в минуту) удаляет просроченные аккаунты.
- Эндпоинты: `POST /auth/login|register|recover`, `POST /admin/reset-password`, `GET /ping`.
- HTTP-маппинг в `GlobalExceptionHandler`: 423/403/410/404.
- Тесты: AuthServiceTest, AdminServiceTest, AuthControllerTest — всего **134 зелёных**.
- Проверено вживую против MinIO+Postgres: все сценарии + лок-аут + recovery.

### Блоки 7-9 ✅ — изображения (скорость)
- **Блок 7 (backend):** при загрузке сервер делает Full (≤1920 по большей стороне)
  и Thumbnail (~144px), обе в JPEG, кладёт в MinIO. Оригинал НЕ хранится
  (`ImageProcessor` порт + `ThumbnailatorImageProcessor`). `GET /photos/{key}?size=thumb|full`.
  Проверено: 4000×3000/796КБ → full 1920×1440/199КБ + thumb 144×108/3.6КБ.
- **Блок 8 (iOS):** `ImageCompressor` — даунскейл до ≤1920 + JPEG (подбор качества к ≤800КБ)
  перед отправкой; EXIF captured_at читается из оригинала ДО сжатия.
- **Блок 9 (iOS):** `ImageStore` (actor) — memory `NSCache` + disk (Caches), ключ SHA256.
  `CachedImage` (память→диск→сеть, повторно не качает). Плитки каталога — thumbnail;
  тап → `PhotoViewer` полноэкранно в full (≤FullHD).
- Тесты: `ThumbnailatorImageProcessorTest` (ресайз/превью/без апскейла) — всего 137 зелёных.

### Блоки 5-6 ✅ — соединение (iOS)
- `ConnectionMonitor` (@MainActor): heartbeat `GET /ping` каждые 5с (async/await),
  состояния ONLINE(<10с)/DEGRADED(10–20с)/OFFLINE(>20с), старт/стоп по scenePhase.
- `ConnectionOverlay` (модификатор на RootView):
  - ONLINE после подключения/реконнекта → зелёный баннер «Подключение установлено» 1.2с;
  - DEGRADED → жёлтая шапка с двумя вращающимися полосками;
  - OFFLINE → помутнение всего экрана + блок кликов, красный треугольник
    «Нет соединения с сервером», снизу вращающиеся серые полоски + «подключение...» +
    капсула «Подключение к серверу» (загрузка фото заблокирована оверлеем).
- **Dynamic Island**: `IslandStatus` — чёрная капсула-«остров» с цветным кружком
  состояния (зелёный/жёлтый/красный), детект DI по верхнему safe-area inset ≥59.
- Хаптики (`Haptics`): success при реконнекте, warning при обрыве.
- Проверено в симуляторе: OFFLINE-блокер (мёртвый URL) и возврат в ONLINE (живой backend).

### iOS-вход ✅ — клиентская часть авторизации
- `LoginView` (заменил регистрацию по нику): шаги Вход → «Создать аккаунт»
  (подтверждение пароля при NOT_FOUND) → «Новый пароль» (RECOVERY).
- `APIClient`: login → `LoginOutcome` (ok/recovery/notFound/invalid/locked,
  читает тела 401/404/423 без выброса), register (409→ошибка), recover.
- Хаптики: success при входе/создании, warning при ошибке.
- **Плавность без рывков:** `BusyController` + `busyOverlay` — при ожидании ответа
  экран замутняется (blur+dim нарастают со временем, `TimelineView(.animation)`),
  спиннер вращается всё медленнее (угол ~√t); следующий экран раскрывается
  только после ответа. Смена Login↔ScanFlow — плавный `.opacity` (0.5с).
- Проверено в симуляторе: экран входа рендерится; backend-сценарии — ранее живьём.

### TODO (следующие ходы)
- Блок 11-12: контекстное меню источника у касания + шестерёнка «режим фото».
- Хаптики на успешном скане ШК; (опц.) Live Activity для реального Dynamic Island.
