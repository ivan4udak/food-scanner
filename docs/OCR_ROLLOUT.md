# OCR rollout — runbook (только staging)

⚠️ **Не трогать stable/production.** OCR активируется ТОЛЬКО на staging до доказательства
контура на заглушке движка. По умолчанию `OCR_AMQP_ENABLED=false` — брокер не подключается.

Контур: upload INGREDIENTS/NUTRITION → `ocr_jobs` QUEUED → publish (RabbitMQ `ocr.jobs`)
→ ocr-service (заглушка → NEEDS_REVIEW) → `ocr.results` → backend listener → статус в БД.

## 0. Проверка ресурсов (сервер 2 ядра/огранич. RAM)
```bash
free -m; docker stats --no-stream
```
RabbitMQ (~150–250 МБ) + ocr-service (заглушка ~50–80 МБ). Если свободно <300 МБ —
**оставить OCR disabled**, зафиксировать причину, вернуться при апгрейде сервера.

## 1. Включение (staging)
1. Синхронизировать серверный `/opt/foodscanner/staging/docker-compose.yml` с
   `deploy/compose/docker-compose.staging.yml` (добавлены сервисы `rabbitmq` + `ocr`).
2. В `/opt/foodscanner/staging/app.env` добавить (см. `deploy/env/.env.staging.example`):
   ```
   OCR_AMQP_ENABLED=true
   SPRING_RABBITMQ_HOST=rabbitmq-staging
   SPRING_RABBITMQ_USERNAME=ocr
   SPRING_RABBITMQ_PASSWORD=<pass>
   RABBITMQ_DEFAULT_USER=ocr
   RABBITMQ_DEFAULT_PASS=<pass>
   RABBIT_URL=amqp://ocr:<pass>@rabbitmq-staging:5672/
   ```
3. Поднять:
   ```bash
   cd /opt/foodscanner/staging
   export BACKEND_IMAGE=<...> WEB_IMAGE=<...> OCR_IMAGE=ghcr.io/ivan4udak/food-scanner-ocr:staging
   docker compose pull && docker compose up -d
   ```

## 2. Проверка (smoke)
```bash
deploy/scripts/ocr-smoke.sh staging   # или вручную ниже
```
- backend healthy с AMQP: `docker logs backend-staging | grep -i rabbit` (нет фатала).
- ocr-service: `docker logs ocr-staging | grep consuming` → `[ocr] consuming ocr.jobs`.
- Загрузить фото `INGREDIENTS` через приложение → проверить БД:
```sql
SELECT status, count(*) FROM food_catalog.ocr_jobs GROUP BY status;  -- ждём 2 (NEEDS_REVIEW)
SELECT id, photo_type, status, attempts, updated_at, error_message
  FROM food_catalog.ocr_jobs ORDER BY updated_at DESC LIMIT 5;
```
Ожидаемо: QUEUED(0) → быстро NEEDS_REVIEW(2) (заглушка). attempts=1.

## 3. Логи
```bash
docker logs -f ocr-staging        # [ocr] consuming / consumed
docker logs backend-staging | grep -iE 'ocr|rabbit|amqp'
docker logs rabbitmq-staging
```

## 3a. Движок: EasyOCR (v1.11.0) vs заглушка
ocr-service выбирает движок по `OCR_ENGINE` (в app.env / compose env): `easyocr` (по умолчанию) либо `stub`.
EasyOCR (CPU): скачивает фото из MinIO (тот же `MINIO_*`/bucket, что backend), распознаёт `rawText`+confidence,
модели качаются лениво на первой задаче (лог `[ocr] lazy-loading EasyOCR models`). Память ограничена `mem_limit: 1800m`.
Параметры (env, дефолты): `OCR_WORKER_CONCURRENCY=1`, `OCR_LANGS=ru,en`, `OCR_JOB_TIMEOUT_SECONDS=120`,
`OCR_IMAGE_DOWNLOAD_TIMEOUT_SECONDS=30`.

Статусы результата (raw-only, парсинга нет):
- текст найден → **NEEDS_REVIEW(2)** + rawText + confidence;
- текста нет / очень низкая уверенность → **PHOTO_UNREADABLE(3)** (errorCode `PHOTO_UNREADABLE`);
- MinIO/decode/timeout/исключение → **ERROR(5)** (`MINIO_OBJECT_NOT_FOUND`/`MINIO_DOWNLOAD_ERROR`/`IMAGE_DECODE_ERROR`/`OCR_TIMEOUT`/`OCR_ENGINE_ERROR`).
`SUCCESS(4)` НЕ ставится за сырой текст — резерв под распарсенный результат.

RAM/CPU: `free -m; docker stats --no-stream ocr-staging`. Проверка результата:
`docker logs ocr-staging | grep -E "lazy-loading|done status"`; в БД `length(raw_text)`, `confidence`, `status`.

## 4. Выключение / откат
```bash
cd /opt/foodscanner/staging
# (a) быстрый откат движка на заглушку (без выключения очереди):
sed -i 's/^OCR_ENGINE=easyocr/OCR_ENGINE=stub/' app.env   # или добавить OCR_ENGINE=stub
docker compose up -d ocr

# (b) полностью выключить OCR:
sed -i 's/^OCR_AMQP_ENABLED=true/OCR_AMQP_ENABLED=false/' app.env
docker compose up -d backend       # backend без AMQP
docker compose stop ocr rabbitmq   # остановить очередь и сервис
```
Данные `ocr_jobs` остаются (для анализа). Полный откат — `docker compose rm -sf ocr rabbitmq`.

## 5. Известные ограничения (до доработки)
- `ocr_jobs.draft_id` без FK-cascade: orphan-строки помечаются `orphaned` в cleanup (v1.10.4).
- EasyOCR на shared-сервере тяжёл по RAM/CPU; `mem_limit: 1800m` ограждает stable/production от OOM
  (при превышении Docker убьёт ocr-staging). concurrency=1/prefetch=1 — не берём пачку фото.
- Парсинг состава/КБЖУ ещё не сделан → текст всегда NEEDS_REVIEW(2), не SUCCESS(4).
- IN_PROGRESS_READABLE(1) промежуточно не публикуется (одно result-сообщение на задачу).

## 6. Статусы (см. docs/OCR.md): 0 QUEUED · 1 IN_PROGRESS · 2 NEEDS_REVIEW · 3 UNREADABLE · 4 SUCCESS · 5 ERROR.
