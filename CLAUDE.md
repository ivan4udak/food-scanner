# Food Scanner — память проекта (для агента)

Каталогизация продуктов по ШК: скан → черновик → 4 обязательных фото → запись каталога.
Backend Spring Boot 3 / Java 21 / Postgres / Flyway / MinIO / JWT, DDD + Ports&Adapters, ArchUnit.
Клиенты: **PWA (основной)** React18+TS+Vite (TanStack Query, Zustand, Axios, Zod, Workbox); iOS — legacy.

## Окружения и поток
`test → staging` (foodscanner-staging.duckdns.org) · `main → stable` (foodscanner-preprod...) · `release → production` (foodscanner...).
Фичи: ветка `feat/*`/`fix/*` от `test` → merge `--no-ff` в `test` → деплой staging → по подтверждению в `main`, в `release` только с явного согласия.
Тесты: backend `mvn` (JDK21 `$(/usr/libexec/java_home -v 21)`); фронт в docker `node:20-alpine` (нет node на хосте). Testcontainers IT поднимают Postgres.

## Версионирование (СТРОГО)
Источник истины: `VERSION` (корень) = `web/package.json` = `web/src/version.ts` = верх `CHANGELOG.md`. Git-теги заморожены на v1.4.0 — НЕ ориентир.
Перед бампом проверить все 4 источника, взять максимум, **никогда не переиспользовать версию**. PATCH=фикс, MINOR=фича/API.
**Текущая: 1.10.6.** Следующая фикс → 1.10.7.

## Git author policy
Коммиты только от владельца (`Volk <m-ore@list.ru>`). НЕ добавлять Co-Authored-By: Claude / Generated with Claude / любые AI-пометки. Перед коммитом сверять `git config user.name/email`.

## Telegram deploy (deploy/scripts/lib.sh — единая карта env→URL)
Сообщение содержит Environment/Branch/Version/Commit + Service URL + API URL + текст коммита («Changes:»). Plain text (без parse_mode).

## Роли
USER / ADMIN / SUPER_ADMIN. Логины из `ADMIN_SUPER_USERNAMES` (деф. `admin`)→SUPER_ADMIN, `ADMIN_USERNAMES`→ADMIN (бутстрап при входе, кладётся в JWT). Гард `/api/v1/admin/**` (AdminGuardInterceptor). Менять роли может только SUPER_ADMIN (`POST /admin/users/{id}/role`).

## Дорожная карта (по ТЗ)
- v1.7 — серверные клиентские логи, телеметрия, correlation-трейс, публичный `/stats`. ✅
- v1.8 — админка (`/admin`: dashboard/users/logs/catalog/errors/trace), «Мои сканы», роли. ✅ (+1.8.1–1.8.7 доработки)
- v1.9 — расширенная аналитика; quality score убран (1.9.1, несправедлив до OCR). ✅
- v1.10 — OCR-микросервис (RabbitMQ + EasyOCR за портом OcrEngine, статусы 0–5, raw text + parsed ingredients/nutrition).
  - v1.10.0 ✅ контракт + ocr_jobs + enqueue QUEUED · v1.10.1 ✅ ocr-service + очередь под флагом `OCR_AMQP_ENABLED`.
  - **Активирован на staging** (заглушка): rabbitmq-staging + ocr-staging, контур upload→QUEUED→publish→ocr→NEEDS_REVIEW(2) доказан. Runbook `docs/OCR_ROLLOUT.md`.
  - v1.10.2 ✅ observability — Prometheus `ocr_jobs{status,code}` (/actuator/prometheus → Grafana).
  - v1.10.3 ✅ admin OCR — страница `/admin/ocr` (summary-чипы/фильтры/список) + API `/admin/ocr[/summary]`.
  - v1.10.4 ✅ lifecycle hardening (active/superseded/orphaned) + backpressure (republish, concurrency=1/prefetch=1, queue-метрики) + admin UX (OCR-блоки в catalog/users, кликабельность, live-refresh) + nav-fix (/about returnTo, без цикла).
  - v1.10.5 ✅ OCR readiness в «Мои сканы»: `ocr[]` в `/me/scans/{barcode}` + клиентский блок «Распознавание» (QUEUED=«Ожидает», polling до финала).
  - v1.10.6 ✅ долг закрыт: photoCount черновика (CASE вместо COALESCE) + failsafe → `*IT` (21) теперь в `mvn verify`.
  - Дальше (план пользователя): v1.10.7 OCR reprocess (статусы 3/5 → переотправка), затем OCR result visibility, **v1.11.0 реальный движок EasyOCR**.
- v2.0 — анализ пользы/вреда продукта. ⬜
OCR-код НЕ писать до v1.10; протоколы готовить заранее.

## Статус веток
staging(test)=1.10.6 (OCR активен, заглушка) · stable(main)=1.8.0 · production(release)=без изменений.
Ожидает: продвинуть 1.8.1–1.10.6 в main по подтверждению. **stable/production не трогать.**

## Режим работы
Низкая многословность. Читать только связанные файлы, не пересканировать дерево, не дублировать абстракции, держать границы DDD, минимум правок, TDD. Формат ответа: PLAN / CHANGES / TESTS / RESULT.
