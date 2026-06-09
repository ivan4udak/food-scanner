# Food Scanner — REST API (актуально, сверено с кодом)

Базовый путь: **`/api/v1`**. Формат — JSON (загрузка фото — `multipart/form-data`).
Даты — ISO-8601 (`2026-06-05T10:00:00Z`).

Развёрнутые окружения (полный базовый URL = `<хост>/api/v1`):
- production: `https://foodscanner.duckdns.org`
- stable: `https://foodscanner-preprod.duckdns.org`
- staging: `https://foodscanner-staging.duckdns.org`
- локально: `http://localhost:8080`

## Авторизация (Bearer JWT)
- Заголовок `Authorization: Bearer <accessToken>` обязателен на **всех** эндпоинтах,
  **кроме публичных**: `POST /auth/login`, `POST /auth/register`, `POST /auth/recover`,
  `POST /auth/refresh`, `GET /ping`, `GET /health`.
- `accessToken` — JWT HS256, срок жизни **24 часа**.
- `refreshToken` — **30 дней**, в БД хранится только SHA-256-хэш; при refresh **ротируется**
  (старый становится недействительным).
- Просмотр каталога (`GET /entries/{barcode}`, `GET /photos/...`) доступен **любому
  авторизованному** пользователю. Проверка владельца — только на операциях с черновиком
  (загрузка фото, завершение): чужой/не-OPEN черновик → `422`.

### Формат ошибки
```json
{ "status": 422, "error": "Unprocessable Entity", "message": "...", "details": ["..."], "timestamp": "..." }
```

### Сопоставление ошибок (из GlobalExceptionHandler)
| Ситуация | HTTP |
|----------|------|
| Ошибка валидации тела (`@Valid`) / нечитаемый JSON / неверный аргумент | **400** |
| Нет/битый/просроченный токен | **401** |
| Неверная роль или админ-пароль | **403** |
| Не найдено (черновик, участник, маршрут) | **404** |
| Конфликт (nickname/штрихкод уже существует) | **409** |
| Окно восстановления истекло | **410** |
| Бизнес-правило (не собраны фото, чужой/не-OPEN черновик) | **422** |
| Аккаунт заблокирован (5 неудач входа → бан 24ч) | **423** |
| Метод не поддерживается | **405** |
| Внутренняя ошибка | **500** |

---

## 1. Auth (публичные)

Все ответы — `AuthResponse` с полями `{ status, contributorId, username, accessToken, refreshToken, message }`
(неприменимые поля = `null`).

### POST /auth/login
Тело: `{ "username": "alice", "password": "secret1" }`
- `200` `{ "status":"OK", "contributorId":"…", "username":"alice", "accessToken":"…", "refreshToken":"…" }`
- `200` `{ "status":"RECOVERY", "username":"alice" }` — пароль сброшен админом, нужен новый (см. `/auth/recover`).
- `404` `{ "status":"NOT_FOUND", "message":"Пользователь не найден" }`
- `401` `{ "status":"INVALID", "message":"Неверный логин или пароль" }`
- `423` `{ "status":"LOCKED", "message":"Аккаунт временно заблокирован" }`

### POST /auth/register
Создание аккаунта (или миграция legacy-ника без пароля). Тело: `{ "username":"…", "password":"…" }`
(пароль **4..100** символов).
- `201` `{ "status":"OK", "contributorId":"…", "username":"…", "accessToken":"…", "refreshToken":"…" }`
- `409` логин занят · `400` пароль вне диапазона.

### POST /auth/recover
Новый пароль в окне восстановления (5 минут после админ-сброса). Тело: `{ "username":"…", "password":"новый" }`
- `200` пара токенов (`status:"OK"`) · `410` окно истекло · `404` нет пользователя.

### POST /auth/refresh
Обновление токенов. Тело: `{ "refreshToken":"…" }`
- `200` новая пара (`accessToken`, `refreshToken`); старый refresh аннулирован.
- `401` refresh невалиден/просрочен → клиент делает logout.

---

## 2. Heartbeat и здоровье (публичные)

### GET /ping
`200` `{ "status":"OK", "timestamp":"2026-06-05T10:00:00Z" }`
Клиент опрашивает каждые 5с: ONLINE (<10с) / DEGRADED (10–20с) / OFFLINE (>20с).

### GET /health
`200` `{ "status":"OK"|"DEGRADED", "backend":"UP", "storage":"UP"|"DOWN", "timestamp":"…" }`
`storage` — доступность MinIO (`OK` если UP, иначе `DEGRADED`).

---

## 3. Каталогизация (Bearer)

### POST /scan
Тело: `{ "barcodeValue": "4607038310042" }`
Пользователь берётся из токена. Поле `contributorId` в теле **опционально** и **игнорируется**
(оставлено для обратной совместимости со старым iOS-клиентом).
- `200` `{ "status":"NEW", "draftId":"…" }` — продукта нет, создан черновик.
- `200` `{ "status":"EXISTS", "draftId": null }` — уже в каталоге.
- `400` `barcodeValue` пустой.

> Повторный скан того же ШК при наличии **OPEN-черновика** у пользователя возвращает
> тот же `draftId` (черновик не дублируется).

### GET /drafts/{draftId}
Состояние черновика владельца — для **восстановления уже загруженных фото** на клиенте.
- `200`:
```json
{ "draftId":"…", "barcode":"4607038310042", "status":"OPEN",
  "photos":[ { "type":"BARCODE", "storageKey":"photos/<hash>.jpg", "capturedAt":"…" } ],
  "uploadedCount":1, "requiredCount":4, "missingTypes":["FRONT","INGREDIENTS","NUTRITION"], "complete":false }
```
`photos` — по одному (последнему) фото на тип. `404` нет черновика · `422` чужой черновик.

### POST /drafts/{draftId}/photos  (multipart/form-data)
Загрузка фото в черновик; владелец проверяется по токену. Поля формы:
- `file` — бинарь изображения (**обязательно**)
- `photoType` — один из `BARCODE | FRONT | BACK | INGREDIENTS | NUTRITION | EXTRA` (**обязательно**)
- `capturedAt` — ISO-8601, дата съёмки (опционально)

Сервер уменьшает до **Full ≤1920** и делает **Thumbnail ~144px** (JPEG), дедуплицирует
по SHA-256 (ключ `photos/{hash}.jpg`), оригинал не хранит.
- `200` `{ "uploadedCount":1, "requiredCount":4, "missingTypes":["BARCODE","INGREDIENTS","NUTRITION"], "complete":false }`
- `404` черновик не найден · `422` чужой черновик или не-OPEN · `400` пустой файл/неверный тип.

```bash
curl -X POST $API/drafts/$ID/photos -H "Authorization: Bearer $ACCESS" \
     -F "file=@photo.jpg;type=image/jpeg" -F "photoType=FRONT" \
     -F "capturedAt=2026-05-01T12:34:56Z"
```
Обязательные 4 типа для завершения: `BARCODE, FRONT, INGREDIENTS, NUTRITION`. `BACK/EXTRA` — опционально.

### POST /drafts/{draftId}/complete
Завершение каталога. **Тело не требуется** (владелец — из токена).
- `201` `{ "catalogEntryId":"…", "contributorCompletedCount": 3 }`
- `404` нет черновика · `422` не собраны все обязательные фото (`details` = список недостающих типов).

### GET /entries/{barcode}
Запись каталога по штрихкоду (любой авторизованный).
- `200`:
```json
{ "id":"…", "barcode":"4607038310042", "contributorId":"…",
  "photos":[ { "id":"…", "type":"FRONT", "storageKey":"photos/<hash>.jpg",
              "capturedAt":"2026-05-01T12:34:56Z" } ],
  "createdAt":"…" }
```
- `404` не найдено.

### GET /photos/{objectKey...}?size=thumb|full
Отдаёт изображение. `objectKey` — это `storageKey` из ответа `entries[].photos[].storageKey`
(формат `photos/<hash>.jpg`), поэтому путь запроса — `/photos/photos/<hash>.jpg`.
По умолчанию `full`; при `size=thumb` сервер берёт ключ превью (`photos/<hash>_thumb.jpg`).
**Требует Bearer** (фото не публичны напрямую).
```bash
curl -H "Authorization: Bearer $ACCESS" "$API/photos/photos/<hash>.jpg?size=thumb" -o thumb.jpg
```

---

## 4. Contributors (legacy, Bearer)

### POST /contributors
Историческая регистрация участника по нику (до системы логин/пароль). Тело: `{ "nickname":"alice" }`
- `201` `{ "contributorId":"…", "nickname":"alice" }` · `409` ник занят.
> Для новых клиентов используйте `POST /auth/register`.

---

## 5. Admin (Bearer + роль)

### POST /admin/reset-password
Сброс пароля пользователя администратором. Тело:
```json
{ "role": "volkov", "password": "<ADMIN_PASSWORD>", "username": "friendLogin" }
```
- `200` `{ "status":"RESET", "message":"Пароль сброшен, окно восстановления 5 минут" }`
- `403` неверная роль/админ-пароль · `404` логин не найден.

`ADMIN_PASSWORD` — переменная окружения сервера (не хардкод).

---

## 6. Метрики / Actuator (вне `/api/v1` — путь `/actuator/**`)
- `GET /actuator/health` — состояние приложения (+ liveness/readiness).
- `GET /actuator/info` — информация о сборке.
- `GET /actuator/prometheus` — метрики OpenMetrics (JVM/HTTP/пул) с тегом `env`
  (`staging|stable|production`). Скрейпится Prometheus.

---

## 7. Телеметрия клиента (Bearer, v1.7)
`contributorId` берётся из токена. Секреты (password/токены/Authorization/Cookie)
маскируются повторно на сервере и **никогда** не сохраняются. Успешные `GET /ping`
и `/health` в `client_logs`/`server_events` не попадают (heartbeat-шум).

### POST /client-logs/batch
Партия клиентских логов.
```json
{ "sessionId":"uuid", "clientVersion":"1.7.0", "pwaVersion":"1.7.0",
  "logs":[ { "id":"uuid","timestamp":"2026-06-08T10:00:00Z","level":"INFO",
             "category":"SCAN","event":"SCAN_RESULT","message":"Scan result NEW",
             "screen":"ScannerPage","correlationId":"uuid","barcode":"460...",
             "apiMethod":"POST","apiPath":"/api/v1/scan","httpStatus":200,
             "durationMs":180,"metadata":{} } ] }
```
→ `200 { "status":"OK", "accepted": <int> }` (accepted — после отсева шума).

### POST /client/session
Снимок сессии (upsert по `sessionId`): `clientVersion,pwaVersion,browser,os,deviceType,
language,timezone,screenWidth,screenHeight,hardwareConcurrency,deviceMemory,networkStatus,standalone`.
→ `200 { "status":"OK" }`.

### POST /client/activity
Лёгкий heartbeat для online/last-activity: `{ sessionId, screen, online, timestamp }`.
→ `200 { "status":"OK" }`.

**Корреляция:** каждый запрос несёт `X-Correlation-Id` (клиент генерирует UUID; если
не прислан — создаёт backend). Ответ возвращает тот же `X-Correlation-Id`. На сервере
формируется `requestId`; значимые события пишутся в `server_events` для сквозной трассировки.

## 8. Публичная статистика (без авторизации)
### GET /public/stats
```json
{ "totals": {"scans":12000,"catalogEntries":3400,"photos":15000,"contributors":180},
  "today":  {"scans":240,"catalogEntries":80,"photos":310} }
```
### GET /public/leaderboard?period=all|today|week|month&limit=10|50|100
```json
{ "period":"all",
  "items":[ {"rank":1,"username":"ivan","completedEntries":120,"scans":340,
             "uploadedPhotos":500,"score":120} ] }
```
Рейтинг: `score = completedEntries` (затем фото, затем сканы). Скрытые (opt-out) и
legacy-без-username исключены.

## 9. Аккаунт (Bearer)
### POST /me/leaderboard-visibility
`{ "hidden": true|false }` → `200 { "hiddenFromLeaderboard": <bool> }`.
Пользователь скрывает/показывает себя в публичном рейтинге.

### GET /me/scans
Свои сканы (только владельца). →
```json
[ { "barcode":"460...", "scanStatus":"COMPLETED", "catalogEntryId":"uuid",
    "firstScannedAt":"…", "completedAt":"…", "photoCount":4, "ocrStatus":null } ]
```
`scanStatus` ∈ `DRAFT_OPEN | COMPLETED`. `ocrStatus` — задел под v1.10 (всегда `null`,
в UI не показывается).

### GET /me/scans/{barcode}
Детали скана с готовыми URL фото; `404` если скан не найден/не принадлежит пользователю.
```json
{ "barcode":"460...", "catalogEntryId":"uuid", "firstScannedAt":"…", "completedAt":"…",
  "photos":[ { "id":"uuid","type":"FRONT","storageKey":"photos/h.jpg",
               "thumbUrl":"/api/v1/photos/photos/h.jpg?size=thumb",
               "fullUrl":"/api/v1/photos/photos/h.jpg?size=full","capturedAt":"…" } ],
  "ocrStatus":null }
```
Фото берутся из завершённой записи (`catalog_entry_photos`) либо из черновика (`draft_photos`).

## 10. Админ-панель (Bearer + роль ADMIN/SUPER_ADMIN, v1.8)
Доступ к `/api/v1/admin/**` разрешён только админам (гард `AdminGuardInterceptor`,
роль из токена). Роль назначается по логину из `ADMIN_USERNAMES` (по умолчанию `admin`)
при входе. Все эндпоинты — только чтение (операционная наблюдаемость).

- `GET /admin/dashboard` → сводка: `usersTotal, onlineNow, activeToday, activeWeek,
  scansToday, scansWeek, entriesToday, entriesWeek, photosToday, clientErrorsToday,
  serverErrorsToday`. (online = активность за 5 мин; today = с UTC-полуночи.)
- `GET /admin/users?sort=&limit=&offset=` → список пользователей: `id, username, role,
  online, lastActivityAt, clientVersion, browser, os, deviceType, totalScans,
  completedEntries, uploadedPhotos, clientErrors`. `sort` ∈
  `lastActivityAt|completedEntries|uploadedPhotos|totalScans|clientErrors`.
- `GET /admin/users/{id}` → карточка: `{ user, sessions[], recentScans[] }`; 404 если нет.
- `GET /admin/users/by-username/{username}` → та же карточка по нику (для перехода
  из публичного рейтинга `/stats`); 404 если не найден.
- `GET /admin/users/{id}/logs?limit=&offset=` → клиентские логи пользователя.
- `GET /admin/users/{id}/errors?limit=` → только WARN/ERROR клиента этого пользователя.
- `POST /admin/users/{id}/role` `{ "role":"USER|ADMIN|SUPER_ADMIN" }` → смена роли. **Только SUPER_ADMIN** (ADMIN → 403). Логины из `ADMIN_SUPER_USERNAMES` (по умолчанию `admin`) — супер-админы; из `ADMIN_USERNAMES` — админы.
- `GET /admin/logs?contributorId=&sessionId=&level=&category=&event=&barcode=&screen=
  &dateFrom=&dateTo=&limit=&offset=` → клиентские логи с фильтрами (полный контекст,
  включая `metadataJson`, `correlationId`).
- `GET /admin/errors?limit=` → `{ client: [клиентские WARN/ERROR за сегодня],
  server: [серверные WARN/ERROR за сегодня] }`.
- `GET /admin/catalog?limit=&offset=` → записи каталога: `catalogEntryId, barcode,
  contributorId, author, createdAt, photoCount`.
- `GET /admin/catalog/{barcode}` → деталь: `{ …, photos[type,storageKey,capturedAt],
  relatedLogs[] }`; 404 если нет.
- `GET /admin/trace/{correlationId}` → **сквозная трассировка**: client_logs +
  server_events в одной временной линии (`source=CLIENT|SERVER, at, level, category,
  event, message, method, path, httpStatus, durationMs`), отсортировано по времени.

---

## Серверные процессы
- **Очистка мусора** (раз в час): незавершённые черновики (OPEN/ABANDONED) старше 24ч удаляются
  вместе с их фото в MinIO (объект удаляется, только если на него больше нет ссылок).
- **Удаление recovery** (раз в минуту): аккаунт без пароля, не задавший новый в окне 5 минут, удаляется.

## Переменные окружения сервера
`DB_URL/DB_USER/DB_PASSWORD`, `MINIO_ENDPOINT/MINIO_ACCESS_KEY/MINIO_SECRET_KEY/MINIO_BUCKET`,
`ADMIN_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TTL_HOURS` (24), `JWT_REFRESH_TTL_DAYS` (30),
`CLEANUP_DRAFT_TTL_HOURS` (24), `APP_ENV` (тег метрик), `MANAGEMENT_ENDPOINTS` (по умолчанию `health,info,prometheus`).

## Типовой сценарий (curl)
```bash
API=https://foodscanner.duckdns.org/api/v1
# 1) создать аккаунт / войти
R=$(curl -s -X POST $API/auth/register -H 'Content-Type: application/json' -d '{"username":"u","password":"pass1234"}')
ACCESS=$(echo "$R" | jq -r .accessToken)
# 2) скан → draftId
D=$(curl -s -X POST $API/scan -H "Authorization: Bearer $ACCESS" -H 'Content-Type: application/json' \
     -d '{"barcodeValue":"4607038310042"}' | jq -r .draftId)
# 3) 4 обязательных фото
for T in BARCODE FRONT INGREDIENTS NUTRITION; do
  curl -s -o /dev/null -X POST $API/drafts/$D/photos -H "Authorization: Bearer $ACCESS" \
       -F "file=@photo.jpg;type=image/jpeg" -F "photoType=$T"
done
# 4) завершить
curl -s -X POST $API/drafts/$D/complete -H "Authorization: Bearer $ACCESS"
# 5) посмотреть каталог
curl -s -H "Authorization: Bearer $ACCESS" $API/entries/4607038310042
```
