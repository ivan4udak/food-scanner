# Food Scanner — подключения и API (по окружениям)

Сервер: `103.119.19.181` (Ubuntu, 4 ГБ / 2 ядра / 30 ГБ). Наружу публично открыт
**только Caddy** (80/443). Всё остальное (БД, MinIO, Prometheus, Grafana-порт) —
на `127.0.0.1`, доступ через **SSH-туннель**.

## Окружения

| Окружение | Ветка | Публичный адрес (Caddy, HTTPS) | API base | web на хосте | Статус |
|-----------|-------|-------------------------------|----------|--------------|--------|
| **staging** | `test` | `https://foodscanner-staging.duckdns.org` | `…/api/v1` | `127.0.0.1:10690` | активно |
| **production** | `release` | `https://foodscanner.duckdns.org` | `…/api/v1` | `127.0.0.1:10890` | активно |
| **preprod** | `main` | `https://foodscanner-preprod.duckdns.org` | `…/api/v1` | `127.0.0.1:10790` | **отключено** (профиль 2-х окружений под текущее железо; можно вернуть) |

Изоляция: у каждого окружения **своя БД** (`postgres-staging` / `postgres-production`)
и **свой bucket** в общем MinIO (`food-images-staging` / `food-images-production`).
Общие на всех: MinIO (один инстанс), Caddy, мониторинг.

---

## 1. Сайт (PWA)
Просто открыть в браузере публичный адрес окружения. Устанавливается на телефон:
Safari → «Поделиться» → «На экран Домой». Камера сканера требует HTTPS — он есть.
- Прод: `https://foodscanner.duckdns.org`
- Staging: `https://foodscanner-staging.duckdns.org`

## 2. iOS-приложение
В приложении на экране входа (или настройках сервера) задаётся **базовый адрес сервера**.
Клиент сам добавляет `/api/v1`, поэтому в поле вписывается БЕЗ него:
- Прод: `https://foodscanner.duckdns.org`
- Staging: `https://foodscanner-staging.duckdns.org`

> iOS требует HTTPS (App Transport Security). DuckDNS + Let's Encrypt это обеспечивают —
> дополнительных исключений в Info.plist не нужно.

## 3. База данных (PostgreSQL)
Порт **не опубликован** наружу (только внутренняя docker-сеть окружения). Варианты:

**a) CLI прямо на сервере (просто):**
```bash
ssh root@103.119.19.181
docker exec -it postgres-production psql -U foodscanner -d foodscanner   # прод
docker exec -it postgres-staging    psql -U foodscanner -d foodscanner   # staging
```

**b) GUI с ноутбука (DBeaver/TablePlus) через SSH-туннель.** Временно опубликуем порт
контейнера на loopback сервера, затем туннель:
```bash
# на сервере (разово, для доступа): пробросить порт контейнера на 127.0.0.1
ssh root@103.119.19.181 'docker exec postgres-production sh -c "true"'   # убедиться, что жив
# туннель: локальный 5433 → контейнер postgres-production:5432 через сеть docker
ssh -L 5433:postgres-production:5432 root@103.119.19.181
```
> `ssh -L 5433:postgres-production:5432` работает, т.к. имя контейнера резолвится в docker-сети
> на стороне сервера. Затем в GUI: host `localhost`, port `5433`, db `foodscanner`,
> user `foodscanner`, пароль = `DB_PASSWORD`/`POSTGRES_PASSWORD` из `…/app.env` окружения.
> Если имя не резолвится — узнать IP: `docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' postgres-production` и тунелить на него.

Креды БД лежат на сервере: `/opt/foodscanner/<env>/app.env` (`DB_USER`, `DB_PASSWORD`).

## 4. MinIO (объектное хранилище, общий инстанс)
- S3-API: внутри docker-сети `minio-shared:9000` (наружу не публикуется).
- Веб-консоль: `127.0.0.1:9001` на сервере → туннель:
```bash
ssh -L 9001:localhost:9001 root@103.119.19.181
# затем открыть http://localhost:9001  (логин = MINIO_ROOT_USER / MINIO_ROOT_PASSWORD из /opt/foodscanner/minio/app.env)
```
Buckets: `food-images-staging`, `food-images-production`. Ключи объектов: `photos/<sha256>.jpg`.

## 5. Мониторинг
- **Grafana:** `https://foodscanner-mon.duckdns.org` (через Caddy, HTTPS). Логин `admin` / пароль из Grafana.
  Дашборд: Dashboards → «Food Scanner» → «Food Scanner — Overview», вверху селектор `env`.
- **Prometheus:** `127.0.0.1:9090` на сервере → туннель: `ssh -L 9090:localhost:9090 root@103.119.19.181` → `http://localhost:9090`.
  Цели: `/targets`. Метрики backend — `/actuator/prometheus` (тег `env=staging|production`).

## 6. SSH / сервер
```bash
ssh root@103.119.19.181
docker ps                              # все контейнеры
docker logs -f backend-production      # логи backend
/opt/foodscanner/scripts/rollback.sh production   # откат к предыдущему релизу
```

---

## API (REST, `/api/v1`)

Базовый URL = `<публичный адрес окружения>/api/v1`. Формат — JSON (загрузка фото — multipart).
Авторизация — `Authorization: Bearer <accessToken>` на всех эндпоинтах, **кроме публичных**
(`auth/*`, `ping`, `health`).

### Auth (публичные)
| Метод | Путь | Тело | Ответы |
|------|------|------|--------|
| POST | `/auth/login` | `{username, password}` | 200 OK `{status:"OK", contributorId, username, accessToken, refreshToken}` · 200 `{status:"RECOVERY"}` · 401 INVALID · 404 NOT_FOUND · 423 LOCKED |
| POST | `/auth/register` | `{username, password(4..100)}` | 201 (пара токенов) · 409 занят · 400 пароль |
| POST | `/auth/recover` | `{username, password}` | 200 (токены) · 410 окно истекло · 404 |
| POST | `/auth/refresh` | `{refreshToken}` | 200 `{accessToken, refreshToken}` · 401 (→ logout) |

`accessToken` — JWT, 24 ч. `refreshToken` — 30 дней, ротируется при refresh.

### Heartbeat / здоровье (публичные)
| Метод | Путь | Ответ |
|------|------|-------|
| GET | `/ping` | `{status:"OK", timestamp}` |
| GET | `/health` | `{status:"OK"|"DEGRADED", backend:"UP", storage:"UP"|"DOWN", timestamp}` (storage = доступность MinIO) |

### Каталогизация (Bearer)
| Метод | Путь | Тело | Ответы |
|------|------|------|--------|
| POST | `/scan` | `{barcodeValue}` | 200 `{status:"NEW", draftId}` · 200 `{status:"EXISTS", draftId:null}` |
| POST | `/drafts/{draftId}/photos` | multipart: `file`, `photoType`, `capturedAt?` | 200 `{uploadedCount, requiredCount, missingTypes[], complete}` · 404 · 422 чужой/не-OPEN · 400 |
| POST | `/drafts/{draftId}/complete` | — (тело не нужно) | 201 `{catalogEntryId, contributorCompletedCount}` · 404 · 422 не все фото |
| GET | `/entries/{barcode}` | — | 200 `{id, barcode, contributorId, photos[], createdAt}` · 404 |
| GET | `/photos/{storageKey}?size=thumb\|full` | — | бинарь изображения (Bearer обязателен) |

- Пользователь во всех операциях берётся **из JWT-токена** (поле в теле игнорируется).
  Историческое: DTO `/scan` раньше требовал `contributorId` — теперь он опционален
  (можно не присылать). iOS присылает его — это допустимо.
- `photoType`: `BARCODE | FRONT | BACK | INGREDIENTS | NUTRITION | EXTRA`.
  Обязательны для завершения: `BARCODE, FRONT, INGREDIENTS, NUTRITION`.
- `storageKey` из ответа `entries[].photos[].storageKey` (формат `photos/<sha>.jpg`),
  путь запроса — `/photos/photos/<sha>.jpg` (storageKey уже включает префикс).

### Admin (Bearer + роль)
| Метод | Путь | Тело | Ответы |
|------|------|------|--------|
| POST | `/admin/reset-password` | `{role:"volkov", password:"<ADMIN_PASSWORD>", username}` | 200 RESET · 403 · 404 |

### Метрики (для Prometheus)
| Метод | Путь | Ответ |
|------|------|-------|
| GET | `/actuator/prometheus` | метрики в формате OpenMetrics (JVM/HTTP/pool, тег `env`) |
| GET | `/actuator/health` | состояние приложения (liveness/readiness) |

### Коды ошибок
400 валидация · 401 нет/битый токен · 403 админ · 404 не найдено · 409 конфликт ·
410 окно восстановления истекло · 422 бизнес-правило · 423 аккаунт заблокирован · 500.
Формат: `{status, error, message, details?, timestamp}`.

### Быстрый пример (curl, прод)
```bash
API=https://foodscanner.duckdns.org/api/v1
R=$(curl -s -X POST $API/auth/register -H 'Content-Type: application/json' -d '{"username":"u","password":"pass1234"}')
ACCESS=$(echo "$R" | jq -r .accessToken)
D=$(curl -s -X POST $API/scan -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' -d '{"barcodeValue":"4607038310042"}' | jq -r .draftId)
for T in BARCODE FRONT INGREDIENTS NUTRITION; do
  curl -s -o /dev/null -X POST $API/drafts/$D/photos -H "Authorization: Bearer $ACCESS" -F "file=@photo.jpg;type=image/jpeg" -F "photoType=$T"
done
curl -s -X POST $API/drafts/$D/complete -H "Authorization: Bearer $ACCESS"
curl -s -H "Authorization: Bearer $ACCESS" $API/entries/4607038310042
```
