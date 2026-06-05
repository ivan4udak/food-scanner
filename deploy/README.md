# Food Scanner — CI/CD (один сервер)

Production-ready конвейер для одного Ubuntu: **GitHub Actions → GHCR → Docker Compose**,
бесплатный HTTPS (**Caddy + Let's Encrypt / DuckDNS**), in-place деплой с health-check и
авто-rollback, **Prometheus + Grafana**, уведомления в **Telegram**. Без Kubernetes/Jenkins/Ansible/Terraform.

> Сервер 4GB/2c/30GB. Три окружения, **in-place** деплой (без blue-green — проще и надёжнее
> на одном хосте; кратковременный простой ~30–60с при деплое некритичен).

## Ветка → окружение → хост

| Ветка | Окружение | Хост (Caddy) | web (loopback) | MinIO bucket |
|-------|-----------|--------------|----------------|--------------|
| `test` | staging | `foodscanner-staging.duckdns.org` | `127.0.0.1:10690` | `food-images-staging` |
| `main` | stable | `foodscanner-preprod.duckdns.org` | `127.0.0.1:10790` | `food-images-stable` |
| `release` | production | `foodscanner.duckdns.org` | `127.0.0.1:10890` | `food-images-production` |

Все окружения: своя БД (`postgres-<env>`) + **общий MinIO** (`minio-shared`, отдельный bucket).
Общие сервисы: `minio-shared`, `caddy`, мониторинг.

Конвейер: **Push → Tests → Maven Build → Docker Build → Push (GHCR) → Deploy → Telegram**.
Сборка — только в Actions; сервер тянет готовые образы из GHCR.

## Версионирование (файл `VERSION` в корне → `v<VERSION>` в Telegram)
- **PATCH** — фикс/мелкая доработка: `1.4.0 → 1.4.1 → … → 1.4.10 …`
- **MINOR** — крупная фича / изменение API: `→ 1.5.0`
- Суффикс ветки: `test → v…t`, `main → v…s`, `release → v…`.

## Структура (`deploy/`)
```
compose/   docker-compose.{staging,stable,production,minio,caddy,monitoring}.yml
caddy/     Caddyfile (сайты по {$*_HOST})
monitoring/ prometheus/prometheus.yml + grafana/provisioning + dashboards
env/       .env.*.example (на сервере → app.env, chmod 600)
scripts/   deploy.sh rollback.sh health-check.sh notify-telegram.sh lib.sh bootstrap-server.sh
systemd/   foodscanner-{minio,caddy,monitoring}.service
logrotate/ foodscanner
```
Образы: `ghcr.io/<owner>/food-scanner-backend` и `-web`, теги `:<sha>` (неизменяемый) + `:<env>`.
Подробные подключения (PWA/iOS/БД/MinIO/Grafana) — см. `deploy/CONNECTIONS.md`.

## 1. GitHub Secrets (Settings → Secrets → Actions)
| Secret | Назначение |
|--------|-----------|
| `SSH_HOST` | IP/домен сервера |
| `SSH_USER` | пользователь деплоя (в группе docker) |
| `SSH_KEY` | приватный SSH-ключ |
| `SSH_PORT` | порт SSH (обычно 22) |
| `TELEGRAM_BOT_TOKEN` | токен бота (опц.) |
| `TELEGRAM_CHAT_ID` | chat id (опц.) |

`GITHUB_TOKEN` — встроенный, используется для push/pull в GHCR (та же org). Пароли приложения
(`JWT_SECRET`, `ADMIN_PASSWORD`, БД/MinIO) в Secrets **не нужны** — они только в `app.env` на сервере.

Опционально создайте GitHub **Environments** `staging/stable/production` (Settings → Environments)
и навесьте на `production` required reviewers — тогда прод-деплой будет с ручным подтверждением.

## 2. DuckDNS
Поддомены на IP сервера: `foodscanner` (prod), `foodscanner-staging`, `foodscanner-preprod`
(используется окружением **stable**), `foodscanner-mon` (Grafana). Откройте порты 80/443.
Хосты задаются в `caddy/app.env` (`PROD_HOST`/`STAGING_HOST`/`STABLE_HOST`/`MONITORING_HOST`).

## 3. Bootstrap сервера (один раз)
```bash
# скопировать репозиторий/deploy на сервер, затем:
sudo bash deploy/scripts/bootstrap-server.sh /path/to/repo/deploy
```
Скрипт: ставит Docker, создаёт `/opt/foodscanner/*`, сеть `foodscanner-edge`, ufw (22/80/443),
копирует compose/Caddyfile/monitoring/scripts/systemd/logrotate.

Затем создайте секреты (chmod 600) из примеров:
```bash
cp deploy/env/.env.staging.example    /opt/foodscanner/staging/app.env
cp deploy/env/.env.production.example /opt/foodscanner/production/app.env
cp deploy/env/.env.caddy.example      /opt/foodscanner/caddy/app.env
cp deploy/env/.env.monitoring.example /opt/foodscanner/monitoring/app.env
chmod 600 /opt/foodscanner/*/app.env
# заполните реальными значениями; для Grafana basic-auth:
docker run --rm caddy:2-alpine caddy hash-password --plaintext 'ПАРОЛЬ'
```
Поднимите edge-инфраструктуру:
```bash
sudo systemctl enable --now foodscanner-caddy foodscanner-monitoring
```

## 4. Деплой
`git push` в `test`/`release` запускает весь конвейер автоматически. Деплой-джоба по SSH
делает `docker login ghcr.io` и запускает `/opt/foodscanner/scripts/deploy.sh <env>`:
- **in-place (оба окружения):** `pull` → `up -d` (пересоздаёт backend) → health-check `backend-<env>`;
  при провале — откат к предыдущему образу из `releases.log`. Кратковременный простой (~30–60с)
  на время старта новой версии.

## 5. Откат вручную
```bash
/opt/foodscanner/scripts/rollback.sh production            # к предыдущему релизу
/opt/foodscanner/scripts/rollback.sh staging <git_sha>     # к конкретному SHA
```
История версий — `/opt/foodscanner/<env>/releases.log` (последние 10).

## 6. Мониторинг
- Grafana: `https://monitoring.<duckdns>` (Caddy basic-auth + логин Grafana). Дашборд «Food Scanner — Overview».
- Prometheus/Grafana на хосте только на loopback (`127.0.0.1:9090/3000`).
- Метрики: Spring Actuator `/actuator/prometheus` (по окружениям), node_exporter (хост), cAdvisor (контейнеры).

## 7. Логи
Docker `json-file` с ротацией (`max-size=10m, max-file=5`) на каждом сервисе + резервный
`logrotate` (`/etc/logrotate.d/foodscanner`). Просмотр: `docker logs -f backend-production`.

## 8. Перезагрузка сервера
App-стеки восстанавливаются автоматически (`restart: unless-stopped`). Caddy и мониторинг
поднимаются systemd-юнитами. Поэтому отдельные systemd-юниты для app-окружений не нужны —
их жизненным циклом управляет деплой из CI.
