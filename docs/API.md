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
