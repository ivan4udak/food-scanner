# Food Scanner

Каталогизация продуктов по штрихкоду: скан → черновик → 4 обязательных фото
(штрихкод, лицевая, состав, КБЖУ) → запись в каталоге. Бэкенд на Spring Boot,
два клиента (PWA — основной, iOS — legacy), хранение фото в MinIO.

## Компоненты
| Папка | Что | Технологии |
|-------|-----|-----------|
| `src/` | Backend (REST `/api/v1`) | Spring Boot 3, Java 21, PostgreSQL, MinIO, JWT, DDD-слои |
| `web/` | **PWA-клиент** (основной) | React 18 + TS 5 + Vite, TanStack Query, Zustand, Zod |
| `ios/` | iOS-клиент (legacy) | SwiftUI, Xcode 16+ |
| `deploy/` | CI/CD на один сервер | GitHub Actions, GHCR, Docker Compose, Caddy, Prometheus/Grafana |

## Развёрнуто (HTTPS, Caddy + Let's Encrypt)
| Окружение | Ветка | Адрес |
|-----------|-------|-------|
| production | `release` | https://foodscanner.duckdns.org |
| stable | `main` | https://foodscanner-preprod.duckdns.org |
| staging | `test` | https://foodscanner-staging.duckdns.org |

PWA на iPhone: Safari → «Поделиться» → «На экран „Домой“» (нужен HTTPS для камеры).

## Документация
- **API:** [`docs/API.md`](docs/API.md) — полный контракт REST (сверен с кодом).
- **Подключения:** [`deploy/CONNECTIONS.md`](deploy/CONNECTIONS.md) — PWA/iOS/БД/MinIO/Grafana + API-таблица.
- **CI/CD runbook:** [`deploy/README.md`](deploy/README.md) — окружения, деплой, откат, версионирование.
- **PWA:** [`web/README.md`](web/README.md). **iOS:** [`ios/README.md`](ios/README.md), [`ios/DEVELOPMENT.md`](ios/DEVELOPMENT.md).
- **История:** [`CHANGELOG.md`](CHANGELOG.md), [`WORKLOG.md`](WORKLOG.md).

## Локальный запуск (backend)
```bash
docker compose up -d                 # Postgres + MinIO
ADMIN_PASSWORD=admin JWT_SECRET=dev-secret-change-me-please-change-me-32b \
  mvn spring-boot:run                # backend на :8080
mvn test                             # 156 тестов
```
PWA локально: `cd web && npm install && VITE_API_TARGET=http://localhost:8080 npm run dev`.

## Версионирование (файл `VERSION` → `v<VERSION>` в Telegram-уведомлениях деплоя)
- **PATCH** — фикс/доработка: `1.4.0 → 1.4.1 → … → 1.4.10 …`
- **MINOR** — крупная фича / изменение API: `→ 1.5.0`

## Ветки → окружения
`test → staging`, `main → stable`, `release → production`. Push в ветку = авто-деплой
соответствующего окружения (Tests → Build → GHCR → Deploy → Telegram).
