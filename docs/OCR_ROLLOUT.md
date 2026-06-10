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

## 4. Выключение / откат
```bash
# мягко: вернуть флаг
sed -i 's/^OCR_AMQP_ENABLED=true/OCR_AMQP_ENABLED=false/' app.env
docker compose up -d backend       # backend без AMQP
docker compose stop ocr rabbitmq   # остановить очередь и сервис
```
Данные `ocr_jobs` остаются (для анализа). Полный откат — `docker compose rm -sf ocr rabbitmq`.

## 5. Известные ограничения (до доработки)
- `ocr_jobs.draft_id` без FK-cascade: при удалении брошенного черновика (cleanup job)
  остаются orphan-строки. Безопасно, но мусор — почистить отдельным срезом.
- Повторная загрузка того же типа фото создаёт новую задачу (старую не отменяет).
- Заглушка движка всегда → NEEDS_REVIEW (фото не читается). Реальный движок — v1.10.2+.

## 6. Статусы (см. docs/OCR.md): 0 QUEUED · 1 IN_PROGRESS · 2 NEEDS_REVIEW · 3 UNREADABLE · 4 SUCCESS · 5 ERROR.
