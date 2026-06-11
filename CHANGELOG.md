# Changelog

Версионирование (источник — файл `VERSION` в корне; CI показывает `v<VERSION>` в Telegram):
- **PATCH** — каждый фикс/мелкая доработка: `1.3.0 → 1.3.1 → … → 1.3.9 → 1.3.10 → 1.3.11 …`
- **MINOR** — новая крупная фича или изменение API: `→ 1.4.0`.
- Суффикс ветки в уведомлении: `test → v…t`, `main → v…s` (stable), `release → v…` (production).

Ветки/окружения: `test → staging`, `main → stable`, `release → production`.

---

## [Unreleased]
- —

## [1.12.6] — client extraction UX («Мои сканы»)
- `GET /me/scans/{barcode}` теперь отдаёт `extraction[]` — последняя задача извлечения на каждый
  OCR-источник (DISTINCT ON ocr_job_id по queued_at, исторические от requeue отброшены):
  photoType/statusCode/status/name/brand/manufacturer/updatedAt. Только свои данные.
- Порт `MeReadPort.extractionForScan` + native SQL (JOIN ocr_jobs ради photoType); маппинг статуса через `ExtractionStatus`.
- Клиент: блок «Данные о продукте» в деталях скана — показываем только содержательные/активные статусы
  (QUEUED/IN_PROGRESS/STRUCTURED/NEEDS_REVIEW; SKIPPED/FAILED скрыты как шум заглушки),
  поля name/brand/manufacturer при наличии, polling 5s пока извлечение активно (как у OCR-блока).
- Без изменения write-path/каталога, без реального LLM/Vision. Тесты: service +2, adapter IT +1, controller обновлён.

## [1.12.5] — admin extraction detail and actions
- `GET /admin/extraction/{jobId}` — полная карточка задачи извлечения + вложенный срез OCR-источника
  (rawText/length/confidence/photoType/storageKey/error). 404 если нет.
- `POST /admin/extraction/{jobId}/requeue` — новая QUEUED-задача с тем же ocrJobId/barcode/type;
  старая остаётся как historical. Разрешено для NEEDS_REVIEW(3)/FAILED(4)/SKIPPED(5), иначе **409**.
- `POST /admin/extraction/{jobId}/skip` — статус → SKIPPED (lastError="Skipped by admin", processedAt=now);
  воркер берёт только QUEUED → пропущенную больше не возьмёт. Разрешено для QUEUED(0)/NEEDS_REVIEW(3)/FAILED(4),
  иначе **409**. IN_PROGRESS(1) — без действий.
- Порт `ProductExtractionJobRepository.findById/skip`; use-cases `RequeueExtractionUseCase`/`SkipExtractionUseCase`.
- Страница `/admin/extraction/:jobId` (статус/связи/структурный результат/OCR-источник + кнопки Переотправить/Пропустить),
  кнопка «детали» в списке. Read-only список из 1.12.4 сохранён.
- Без реального LLM/Vision, без перезаписи ProductCard. Тесты: controller +5, services +6, adapter IT +1.

## [1.12.4] — admin extraction visibility
- Новый admin read-API: `GET /admin/extraction` (фильтры `status`/`type`/`barcode`, свежие сверху)
  + `GET /admin/extraction/summary` (счётчики по статусам 0–5, zero-fill).
- Порт `AdminReadPort.extractionJobs/extractionSummary` + адаптер (native SQL по `product_extraction_jobs`),
  use-case `AdminReadUseCase.extraction/extractionSummary`, маппинг кода статуса → имя через `ExtractionStatus`.
- Страница `/admin/extraction`: чипы-сводка (клик фильтрует) + фильтр по типу (текст/фото) +
  список (ШК→каталог, кнопка OCR→задача-источник, поля name/brand/manufacturer, source, attempts, last_error).
  Вкладка «Извлечение» в admin-навигации.
- Read-only (reprocess/skip — следующим срезом). Тесты: `AdminReadControllerTest` (+2), `AdminReadAdapterIT` (+1).

## [1.12.3] — deploy disk hygiene for OCR images
- `deploy.sh` вызывает `ensure_disk_space` **перед** `docker compose pull`: лог `df`/`docker system df`,
  при свободном <`MIN_FREE_DISK_GB` (деф. 5) — безопасная очистка `docker image/builder/container prune`.
- **volumes НЕ трогаются** (Postgres/MinIO в безопасности); running-образы (stable/production) сохраняются.
- При нехватке места после очистки — деплой прерывается до старта (без частичного деплоя), чёткое сообщение.
- Чинит crash-loop backend из v1.12.2 (диск 100% от тяжёлых EasyOCR/torch образов → No space left on device).
- Runbook (`docs/OCR_ROLLOUT.md` §0) обновлён. bash -n OK; staging only.

## [1.12.2] — OCR reprocess for NEEDS_REVIEW
- `POST /admin/ocr/{jobId}/reprocess` теперь разрешён и для **NEEDS_REVIEW(2)** (после реального EasyOCR
  нормальный raw-text получает статус 2 — админ может перечитать фото: плохой текст/обновлён движок/сравнить качество).
- Статусы **3 PHOTO_UNREADABLE / 5 ERROR** — по-прежнему разрешены; **0/1/4 отклоняются (409)**. Endpoint не менялся.
- Поведение reprocess без изменений (supersede старой + новая QUEUED + publish best-effort). Старые extraction-задачи
  не трогаем — новый OCR-результат сам создаст новую по eligibility.
- Admin UI: «↻ Переотправить» виден для 2/3/5. Тесты: ReprocessOcrService (2/3/5 allow, 0/1/4 reject), фронт.

## [1.12.1] — Product Extraction: ночной воркер + StubProductExtractor
- **`ProductExtractor` порт** + **`StubProductExtractor`** (флаг `PRODUCT_EXTRACTOR=stub|text_llm|image_llm`,
  по умолчанию stub → ничего не извлекает → SKIPPED). Реальные LLM/vision — отдельный срез.
- **`ProductExtractionWorker`** (`@Scheduled`): обработка только в ночном окне (`ProcessingWindow`, через полночь),
  **concurrency=1** (AtomicBoolean-гард), лимиты на окно (max-jobs/max-minutes), runtime-safety (heap<90% иначе пропуск).
  QUEUED→IN_PROGRESS→STRUCTURED/NEEDS_REVIEW/SKIPPED, FAILED при исключении. Вне окна/при перегрузке — задачи остаются QUEUED.
- Repo: `findQueued/markInProgress/applyResult/markFailed` (@Modifying, clearAutomatically). Config `product.extractor.*`.
- Roadmap: **Честный ЗНАК перенесён на v1.13.0** (v1.12.0 уже занята Product Extraction). НЕ реализуется сейчас.
- Тесты: ProcessingWindow (3, вкл. overnight), ProductExtractionWorker (4: skipped/structured/failed/disabled), repo IT (lifecycle). mvn verify = surefire + 24 IT. Без LLM/cloud/Честного ЗНАКа; staging only.

## [1.12.0] — Product Extraction foundation: очередь задач + eligibility (без LLM/воркера)
- **V17**: таблица `product_extraction_jobs` (тип TEXT_EXTRACTION/IMAGE_FALLBACK_EXTRACTION, статусы 0–5
  QUEUED/IN_PROGRESS/STRUCTURED/NEEDS_REVIEW/FAILED/SKIPPED, поля результата name/brand/manufacturer/composition/nutrition/confidence — nullable).
- **Разделение OCR и Extraction**: после применения OCR-результата создаётся задача извлечения по
  доменной `ExtractionEligibilityPolicy`: PHOTO_UNREADABLE/ERROR → IMAGE_FALLBACK; достаточно текста
  (len≥`EXTRACTOR_MIN_RAW_TEXT_LENGTH`=100) и уверенность ≥`EXTRACTOR_MIN_OCR_CONFIDENCE`=0.35 → TEXT_EXTRACTION;
  иначе IMAGE_FALLBACK; нетерминальные/SUCCESS → пропуск. Флаг `PRODUCT_EXTRACTION_ENABLED`.
- DDD: domain (ExtractionStatus/Type, ProductExtractionJob, policy), порт+адаптер, use-case, wired в UpdateOcrResultService.
- **Не делалось** (след. срезы): ночной воркер+окно+runtime-safety, ProductExtractor порт+stub адаптер, admin/UX, реальный LLM/vision. OCR pipeline/публичный API не тронуты.
- Тесты: policy (6), EnqueueProductExtractionService (4), UpdateOcrResultService (enqueue), ProductExtractionJobRepositoryAdapterIT; mvn verify = surefire + 23 IT.

## [1.11.3] — фундамент структурного извлечения (поля, без LLM)
- **V16**: `ocr_jobs` += `parsed_name/parsed_brand/parsed_manufacturer` (nullable, без backfill/constraints).
  composition→`parsed_ingredients`, КБЖУ→`parsed_nutrition` (уже были).
- **API**: `GET /admin/ocr/{jobId}` отдаёт `parsedName/parsedBrand/parsedManufacturer` (null для старых job).
- **Админка**: в `/admin/ocr/:jobId` блок «Структурные данные» (название/бренд/производитель/состав/КБЖУ),
  пустой — аккуратно «Пока не извлечено». Остальные блоки (rawText/фото/reprocess/связи) без изменений.
- **Не делалось**: LLM/extractor, ocr-service, OCR pipeline, write-path, парсинг, semantics статусов. Только чтение/видимость.
- Дальше: ProductExtractor (порт + адаптер) + write-path — отдельным срезом (с решением, где хостить модель).

## [1.11.2] — откат тюнинга OCR (регресс), оставлен EXIF-фикс
- v1.11.1 тюнинг (RGB + пониженные пороги `text_threshold/low_text` + `mag_ratio`) на проверке **ухудшил**
  распознавание (то же фото: 1780→1193 симв, conf 0.60→0.47), нечитаемое не восстановил. Откатили к
  проверенной конфигурации v1.11.0 (grayscale + дефолтные пороги EasyOCR).
- Оставлен безопасный фикс ориентации **`ImageOps.exif_transpose`** (поворот фото с телефона по EXIF).
- Только ocr-service; статусы/контракт не менялись. Вывод: тюнинг OCR требует измеримой выборки, не эвристики.

## [1.11.1] — OCR quality: preprocessing + тюнинг детектора
- EasyOcrEngine: EXIF-ориентация (`ImageOps.exif_transpose`), цвет RGB вместо grayscale,
  внутренний апскейл мелких фото (`mag_ratio`, длинная сторона < `OCR_UPSCALE_BELOW`),
  пониженные пороги детектора (`OCR_TEXT_THRESHOLD=0.6`, `OCR_LOW_TEXT=0.3`) → выше recall.
- Все параметры через env (дефолты безопасные). Только ocr-service; статусы/контракт не менялись.

## [1.11.0] — real OCR: EasyOCR raw text
- **ocr-service**: реальный движок **EasyOCR (CPU-only torch)** за абстракцией `OcrEngine`
  (`EasyOcrEngine`/`StubOcrEngine`, выбор по `OCR_ENGINE=easyocr|stub` — мгновенный откат).
- Скачивание фото из **MinIO** внутри ocr-service по `storageKey` (тот же bucket/креды, что backend; через app.env).
- Языки **ru,en**; per-job timeout (`OCR_JOB_TIMEOUT_SECONDS=120`), download timeout (`30`);
  `OCR_WORKER_CONCURRENCY=1` + RabbitMQ prefetch=1; модели EasyOCR — lazy-load на первой задаче.
- Маппинг статусов (raw-only, без парсинга): текст→**NEEDS_REVIEW(2)**+rawText+confidence;
  нет текста/низкая уверенность→**PHOTO_UNREADABLE(3)**; MinIO/decode/timeout/исключение→**ERROR(5)**
  (`MINIO_OBJECT_NOT_FOUND`/`MINIO_DOWNLOAD_ERROR`/`IMAGE_DECODE_ERROR`/`OCR_TIMEOUT`/`OCR_ENGINE_ERROR`).
  `SUCCESS(4)` НЕ ставится за сырой текст. Контракт `ocr_jobs`/статусы не менялись.
- Безопасность хоста: `mem_limit: 1800m` только на `ocr-staging` (при OOM умрёт OCR, не stable/production).
- Тесты: `python -m unittest test_app` (5, status mapping); `compileall` ok. **Деплой только staging.**
- НЕ в этом релизе: парсинг состава/КБЖУ, scoring, внешние каталоги/маркировка, mass reprocess.

## [1.10.8] — OCR result visibility: карточка задачи /admin/ocr/{jobId}
- **API**: `GET /admin/ocr/{jobId}` — полная карточка (полный rawText/parsedIngredients/parsedNutrition,
  confidence, lifecycle active/superseded/orphaned, publish published_at/attempts/last_publish_error). 404 если нет.
- **Админка**: страница `/admin/ocr/:jobId` — статус с цветом, полный rawText + копирование, фото (lightbox),
  кликабельные ШК→каталог и автор→пользователь, reprocess для 3/5; кнопка «детали» в таблице OCR. Polling до финала.
- Порт `AdminReadPort.ocrById` + native SQL; use-case `ocrDetail`. Иерархия «Назад» для `/admin/ocr/:jobId` уже была.
- Тесты: AdminReadControllerTest (200/404), AdminReadAdapterIT.ocrById; `mvn verify` = surefire + 22 IT; фронт vitest 63.
- Дальше: **v1.11.0 реальный движок EasyOCR** (raw text/confidence; парсинг состава/КБЖУ — позже).

## [1.10.7] — OCR reprocess (статусы 3/5 → переотправка)
- **API**: `POST /admin/ocr/{jobId}/reprocess` — для PHOTO_UNREADABLE(3)/ERROR(5) создаёт новую QUEUED-задачу
  для того же фото (storageKey/photoType/draft/entry), замещает старую (`supersede` по id), публикует.
  200 `{jobId}` / 404 (нет задачи) / 409 (статус не 3/5).
- Use-case `ReprocessOcrUseCase`/`ReprocessOcrService`; `OcrJob.requeue`; `OcrJobRepository.supersede(id, by)`.
- **Админка**: в таблице OCR (страница/каталог/пользователь) у задач 3/5 кнопка «↻ Переотправить» (live-обновление).
- Тесты: ReprocessOcrServiceTest (3), AdminOcrControllerTest (200/404/409), OcrJobRepositoryAdapterIT.supersedeById;
  `mvn verify` = surefire + 22 IT зелёные; фронт vitest 63.

## [1.10.6] — закрытие долга: photoCount черновика + IT в CI-гейте
- **Fix**: `/me/scans` (список) — `photoCount` черновика считался всегда 0 (COALESCE брал count записи=0
  для черновика без entry). Заменено на `CASE WHEN e.id IS NULL THEN draft_photos ELSE entry_photos`.
- **CI-гейт**: добавлен `maven-failsafe-plugin` (integration-test+verify) — `*IT` (Testcontainers) теперь
  гоняются в `mvn verify`. Раньше целый пласт IT не исполнялся (баг photoCount поэтому не ловился).
- Проверено: `mvn verify` = 240 surefire + 21 IT зелёные.

## [1.10.5] — OCR readiness в «Мои сканы» (API + клиент)
- **API**: `GET /me/scans/{barcode}` += `ocr[]` (per photoType: statusCode/status/confidence/updatedAt/error*/rawTextPreview).
  Аддитивно; поле `ocrStatus` сохранено. Порт `MeReadPort.ocrForScan` (active-задачи по draft/entry, CAST для null).
- **Клиент**: блок «Распознавание» в карточке скана с подписями/цветом статуса (QUEUED = «Ожидает распознавания», не ошибка);
  пустой блок не показывается; polling 5s с остановкой на финальном статусе (нет QUEUED/IN_PROGRESS).
- Тесты: MyScansServiceTest (маппинг OCR), MeScansControllerTest, MeReadAdapterIT (ocrForScan active/null); фронт vitest 63.
- Дальше: v1.10.6 OCR result visibility (полный rawText/копирование в админке), v1.11.0 реальный EasyOCR.

## [1.10.4] — OCR lifecycle hardening + backpressure + admin UX
- **Lifecycle** (V15): `ocr_jobs` += active/superseded_*/orphaned_*/published_at/publish_attempts/last_publish_error.
  Supersede — активна только последняя задача (draft, photoType); orphan — задачи удалённых черновиков
  помечаются неактивными в cleanup.
- **Backpressure**: `OcrJobPublisher.publish()→boolean` (best-effort, upload не падает); `OcrRepublishJob`
  переотправляет зависшие QUEUED (под `ocr.amqp.enabled`); ocr-service `prefetch=OCR_WORKER_CONCURRENCY` (деф. 1).
  Метрики `ocr_queue_size`, `ocr_queue_oldest_age_seconds`.
- **Admin OCR**: default-view active & !orphaned + `showInactive/showOrphaned`; summary += queueSize/oldestQueuedAge
  + предупреждение о росте очереди; OCR-блок в `/admin/catalog/{barcode}` и `/admin/users/{id}`;
  кликабельность (ШК→каталог, автор→пользователь); live-refresh + индикатор «Обновлено».
- **Nav-fix**: `/about?returnTo=` + возврат через `replace` — нет цикла /about ↔ admin leaf; иерархический «Назад» (`lib/nav`).
- Тесты: backend 239 surefire + IT (lifecycle/active-view/byBarcode-byUser); фронт vitest 63 (incl nav).
- Дальше: v1.10.5 OCR data/API/client readiness (поля ocr* в /me/scans + клиентский статус «Ожидает распознавания»).

## [1.10.3] — admin OCR: страница /admin/ocr
- **`/admin/ocr`** — наблюдаемость OCR в админке: сводка по статусам (кликабельные чипы-фильтры),
  поиск по ШК, список задач (ШК/тип/статус/попытки/обновлено/ошибка/превью rawText).
- **API:** `GET /admin/ocr?status&barcode&limit&offset` + `GET /admin/ocr/summary`;
  barcode резолвится через draft/entry, rawText обрезается до 200 символов на чтении.
- Порт `AdminReadPort.ocrJobs/ocrSummary` (нативный SQL), use-case + контроллер.
- Тесты: AdminReadControllerTest (+2), AdminReadAdapterIT (резолв barcode/фильтры/summary). Фронт 56 зелёных.
- Дальше: OCR-блок в `/admin/catalog/{barcode}` и `/admin/users/{id}` (v1.10.3.x), затем EasyOCR v1.10.4+.

## [1.10.2] — OCR observability: метрика ocr_jobs by status
- **Prometheus-метрика `ocr_jobs{status,code}`** — количество OCR-задач по каждому статусу
  (QUEUED/IN_PROGRESS_READABLE/NEEDS_REVIEW/PHOTO_UNREADABLE/SUCCESS/ERROR), zero-fill для
  пустых. Значения обновляются периодически (один групповой COUNT), gauge читаются из памяти.
  Видно в `/actuator/prometheus` → Grafana. Работает независимо от `OCR_AMQP_ENABLED`.
- Порт `OcrJobRepository.countByStatus()` + JPQL-проекция в JPA-адаптере.
- Backend +2 теста (OcrMetricsTest unit, OcrJobRepositoryAdapterIT countByStatus).
- Дальше (отдельными срезами): admin OCR (`/admin/ocr`) v1.10.3, затем EasyOCR v1.10.4+.

## [1.10.1] — OCR-сервис и очередь (скелет, под флагом)
- **ocr-service/** — отдельный контейнер Python/FastAPI + pika: health, RabbitMQ-консьюмер
  задач, движок-заглушка (→ NEEDS_REVIEW), публикация результата. Образ собирается в CI.
- **Backend AMQP под флагом `ocr.amqp.enabled` (по умолчанию OFF):** порт `OcrJobPublisher`
  (NoOp по умолчанию / RabbitMQ при флаге), публикация задачи при enqueue,
  `@RabbitListener` результатов → `UpdateOcrResultService` (статус/текст/КБЖУ, attempts+1).
  Без флага брокер не подключается — поведение не меняется.
- **CI/инфра:** 3-й образ food-scanner-ocr (build-push), `OCR_IMAGE` в deploy; шаблон
  compose staging с rabbitmq + ocr (для rollout; серверный compose синкается отдельно).
- Backend 234 теста. **Активация OCR — отдельный rollout** (compose на сервере +
  OCR_AMQP_ENABLED=true + RabbitMQ env).


## [1.10.0] — OCR foundation: контракт и слой данных
- **Контракт OCR** (`docs/OCR.md`): статусы 0–5, RabbitMQ-топология, схемы job/result,
  модель данных. Согласованный стек: RabbitMQ + EasyOCR (за портом OcrEngine), тот же сервер.
- **Backend (foundation, без брокера/движка):** Flyway V14 `ocr_jobs`; domain `OcrStatus`(0–5)
  + `OcrJob`; репозиторий + JPA-адаптер. При загрузке фото `INGREDIENTS`/`NUTRITION`
  создаётся OCR-задача (QUEUED) — `EnqueueOcrService`, хук в `POST /drafts/{id}/photos`.
- Следующий срез (v1.10.1): RabbitMQ + OCR-сервис (FastAPI, заглушка движка) → end-to-end статусы.
- Backend 232 теста (вкл. Testcontainers-IT OcrJob/jsonb).


## [1.9.1] — Убран quality score до OCR
- Оценка качества записи по типам/числу фото признана несправедливой (размер
  упаковки ≠ полнота информации) и удалена из админ-каталога вместе с
  `CatalogQualityPolicy`. Честную оценку читаемости/полноты вернём на v1.10 (OCR).
  Недостающие обязательные типы по-прежнему видны для черновиков (`GET /drafts/{id}`).


## [1.9.0] — Аналитика: качество записи каталога (без OCR)
- **Quality score (без OCR):** доменная политика `CatalogQualityPolicy` — оценка
  записи 0–100 по присутствующим типам фото (BARCODE 20, FRONT 20, INGREDIENTS 25,
  NUTRITION 25, BACK|EXTRA 10).
- **Админка:** в списке каталога — колонка «Кач.», в карточке записи — «Качество N/100».
  Бэкенд считает через array_agg типов фото + политику. Следующие срезы v1.9:
  качество вклада пользователя, агрегаты, подготовка OCR-протоколов.


## [1.8.8] — Фикс кнопки «Очистить кэш»
- «Очистить кэш» теперь реально чистит Cache Storage (Workbox), обновляет SW
  и показывает прогресс/«Очищено ✓»; размер пересчитывается. Логика вынесена в
  тестируемый `lib/cache.ts` (clearAppCaches/refreshServiceWorkers/estimateUsage).


## [1.8.7] — Управление ролями (супер-админ) и фикс «Назад» из «О приложении»
- **Роли:** добавлен SUPER_ADMIN. Логины из `ADMIN_SUPER_USERNAMES` (по умолчанию
  `admin`) получают SUPER_ADMIN при входе; `ADMIN_USERNAMES` — ADMIN.
- **API:** `POST /api/v1/admin/users/{id}/role` — смена роли пользователя. Доступно
  только SUPER_ADMIN; обычный ADMIN получает 403 (заходит в админку, но роли не раздаёт).
- **PWA:** в карточке пользователя супер-админ видит селектор смены роли
  (USER/ADMIN/SUPER_ADMIN). Строка «Ошибок клиента» кликабельна (без кнопки),
  ведёт на страницу ошибок пользователя; в списке — клик по числу «Ош.» без
  изменения оформления.
- **Фикс навигации:** «Назад» из «О приложении» возвращает на страницу, с которой
  открыли по ⚙ (запоминается при нажатии шестерёнки), а не в админку.

## [1.8.6] — Ошибки пользователя в админке
- **API:** `GET /api/v1/admin/users/{id}/errors` — только WARN/ERROR клиентские логи
  конкретного пользователя.
- **PWA:** в списке пользователей клик по числу в колонке «Ош.» (или кнопка
  «Ошибки клиента (N)» в карточке) открывает страницу с ошибками этого пользователя.
  «Назад» с неё ведёт в карточку пользователя.


## [1.8.5] — Иерархический «Назад» в админке и правило подсчёта сканов
- **Навигация админки:** кнопка «Назад» на верхних вкладках (Дашборд/Пользователи/
  Логи/Каталог/Ошибки) ведёт в «О приложении»; из карточки пользователя →
  в список «Пользователи»; из карточки каталога → в список «Каталог»; из
  трассировки — по истории.
- **Подсчёт сканов:** скан засчитывается в статистике только если в черновике есть
  хотя бы одно фото (или черновик завершён в запись). Пустые брошенные черновики
  остаются в БД, но не учитываются как +1 скан и не показываются в списках сканов.
  Затронуты: публичная статистика (totals/today/рейтинг), админ-дашборд,
  «сканов» в карточке пользователя, списки «Мои сканы» и сканов пользователя.


## [1.8.4] — Сканер чувствительнее, табы админки, фильтр логов по пользователю
- **Сканер (PWA):** камера запрашивается в высоком разрешении (ideal 1920×1080) +
  непрерывный автофокус — ШК не нужно подводить вплотную. ZXing-fallback (iOS Safari)
  ускорен: попытки каждые 100мс вместо 500мс (дефолт), ограничен набор форматов
  (EAN/UPC/Code128/39/ITF/QR) + TRY_HARDER. Нативный детектор — ~8 проверок/сек.
- **Админ-табы:** Дашборд/Пользователи/Логи/Каталог/Ошибки в один ряд с
  горизонтальным скроллом — больше не переносятся на 2 строки и не «прыгают»
  при переключении.
- **Логи:** фильтр по пользователю — поле с автоподбором (datalist): список ников
  подстраивается под ввод; по точному совпадению фильтрует логи этого пользователя.
  Добавлена кнопка «Сбросить».


## [1.8.3] — Фикс вёрстки админ-таблиц
- Таблицы админки (пользователи, сканы, сессии, каталог) теперь скроллятся по
  горизонтали внутри карточки — колонки «Фото/Скан./Ош.» больше не уезжают за
  её границы на узких экранах.
- ШК в каталоге и сканах не переносится по символам (вертикально) — ячейки в
  одну строку (`nowrap`), длинное значение доступно горизонтальным скроллом.


## [1.8.2] — Стабильное присутствие, навигация «назад», вёрстка списка
- **Presence (фикс):** онлайн-статус считается по GREATEST(последняя сессия,
  последняя heartbeat-активность). Раньше при незавершённом `POST /client/session`
  пользователь всегда выглядел офлайн, хотя heartbeat шёл. Дашборд-счётчики
  online/active тоже учитывают активность (UNION сессий и активности).
- **Heartbeat чаще:** активность шлётся каждые 30с + сразу при возврате на вкладку
  (`visibilitychange`/`focus`) и восстановлении сети. Админ-список и карточка
  пользователя авто-обновляются каждые 20с — статус не «залипает».
- **Навигация:** кнопка «Назад» во всех экранах (вкл. админ-панель) возвращает на
  предыдущую страницу (`history`), а не на экран сканирования; при прямом заходе —
  на главную.
- **Вёрстка:** в списке пользователей ник и бейдж роли (`ADMIN`) больше не
  переносятся на несколько строк (одна строка с усечением, отдельная строка деталей).

## [1.8.1] — Drill-down из статистики и полноразмерные фото
- **API (аддитивно):** `GET /api/v1/admin/users/by-username/{username}` → карточка
  пользователя по нику (для перехода из публичного рейтинга). 404 если не найден.
- **PWA / дашборд без регистрации:** ссылка «Статистика проекта» на экране входа —
  публичный `/stats` доступен без авторизации.
- **PWA / админ:** на `/stats` для админа ник в рейтинге кликабелен → открывается
  карточка пользователя (`/admin/u/{username}` → редирект на `/admin/users/{id}`)
  с его сканами, сессиями и логами; клик по скану → фото записи. Обычные пользователи
  изменений не видят.
- **PWA / фото:** клик по фото (в админ-каталоге и «Моих сканах») открывает его в
  полном качестве с сервера (полноэкранный просмотр, авторизованная загрузка size=full).

## [1.8.0] — Админ-панель, «Мои сканы», роли
- **Роли (API):** `ContributorRole` (USER/ADMIN/SUPER_ADMIN), Flyway V13 `role`.
  Роль кладётся в JWT и проверяется `AdminGuardInterceptor` на `/api/v1/admin/**`.
  Логины из `ADMIN_USERNAMES` (по умолчанию `admin`) получают ADMIN при входе.
- **Admin read-API (Bearer + ADMIN):** `GET /admin/dashboard` (сводка),
  `/admin/users` (+ карточка `/users/{id}`, логи `/users/{id}/logs`),
  `/admin/logs` (фильтры), `/admin/errors`, `/admin/catalog` (+ деталь `/{barcode}`),
  `/admin/trace/{correlationId}` — **сквозная трассировка** client_logs + server_events
  в одной временной линии.
- **«Мои сканы» (Bearer):** `GET /me/scans`, `GET /me/scans/{barcode}` — пользователь
  видит только свои ШК и фото (thumb/full URL). `ocrStatus` зарезервирован под v1.10.
- **PWA:** админ-панель `/admin` (дашборд, пользователи, логи, каталог, ошибки,
  трассировка; гард по роли из JWT), страница «Мои сканы» `/my-scans`; ссылки в кабинете.
- Backend 216 тестов (вкл. Testcontainers-IT read-адаптеров), фронт — тесты JWT/роли.

## [1.7.2] — Чистка логов и навигация статистики
- **Логи:** успешные `GET /ping` и `GET /health` (heartbeat каждые 5с) больше не пишутся
  в клиентский лог — ни старт-запрос, ни ответ 200. Ошибки/таймауты health-эндпоинтов
  по-прежнему логируются. Локальная диагностика и скачанный лог стали читаемыми
  (раньше шум ping/health забивал последние 100 записей).
- **`/stats`:** кнопка в шапке стала «‹ Назад» и возвращает на страницу, с которой
  открыли статистику (`navigate(-1)`), а не всегда на экран сканирования. При прямом
  заходе (новая вкладка, нет истории) — на главную.

## [1.7.1] — Фикс: чёрный экран камеры на iOS (standalone PWA)
- Камера сканера запускается один раз при монтировании и живёт весь жизненный цикл
  экрана; `paused` (нет сети/идёт запрос) приостанавливает только приём штрихкодов,
  не пересоздавая поток. Раньше при «degraded»/сетевых колебаниях поток рвался и
  пересоздавался — на iOS это оставляло чёрный кадр.
- iOS-специфика видео: `muted`+`playsinline` выставляются как свойства элемента
  (не только атрибуты React), `play()` с обработкой ошибки.
- Диагностика: логи `camera stream acquired` (треки), `video playing` (размеры),
  `video.play() rejected`, имя/сообщение ошибки в `Camera unavailable` —
  чтобы причина была видна в клиентском логе.

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
