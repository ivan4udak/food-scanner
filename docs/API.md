# Food Scanner — REST API (подробная инструкция)

Базовый URL: `http://<host>:8080/api/v1`
Формат: JSON (кроме загрузки фото — `multipart/form-data`).
Кодировки дат: ISO-8601 (`2026-06-04T10:00:00Z`).

## Аутентификация (Bearer JWT)

- Все эндпоинты требуют заголовок `Authorization: Bearer <accessToken>`,
  **кроме** публичных: `POST /auth/login`, `POST /auth/register`,
  `POST /auth/recover`, `POST /auth/refresh`, `GET /ping`.
- `accessToken` — JWT, срок жизни **24 часа**.
- `refreshToken` — срок жизни **30 дней**, в БД хранится только его SHA-256-хэш.
  При обновлении refresh **ротируется** (старый становится недействительным).
- При истёкшем/неверном токене — `401 Unauthorized`.

Просмотр каталога (`GET /entries/{barcode}`, `GET /photos/...`) доступен **любому
авторизованному** пользователю, независимо от того, кто добавил запись.
Проверка владельца применяется только к операциям с черновиком (загрузка фото, завершение).

### Формат ошибки
```json
{ "status": 422, "error": "Unprocessable Entity", "message": "...", "details": ["..."], "timestamp": "..." }
```
Коды: 400 (валидация), 401 (нет/битый токен), 403 (админ), 404 (не найдено),
409 (конфликт/занято), 410 (окно восстановления истекло), 422 (бизнес-правило),
423 (аккаунт заблокирован), 500 (внутренняя).

---

## 1. Auth

### POST /auth/login
Вход. Тело:
```json
{ "username": "alice", "password": "secret1" }
```
Ответы:
- `200` `{ "status":"OK", "contributorId":"…", "username":"alice", "accessToken":"…", "refreshToken":"…" }`
- `200` `{ "status":"RECOVERY", "username":"alice" }` — пароль сброшен админом, нужен новый.
- `404` `{ "status":"NOT_FOUND" }` — пользователя нет (клиент предлагает создать).
- `401` `{ "status":"INVALID", "message":"Неверный логин или пароль" }`
- `423` `{ "status":"LOCKED", "message":"Аккаунт временно заблокирован" }` — после 5 неудач, бан 24ч.

```bash
curl -X POST $API/auth/login -H 'Content-Type: application/json' \
     -d '{"username":"alice","password":"secret1"}'
```

### POST /auth/register
Создание аккаунта (или миграция legacy-ника без пароля). Тело:
`{ "username": "...", "password": "...4..100..." }`
- `201` `{ "status":"OK", "contributorId":"…", "username":"…", "accessToken":"…", "refreshToken":"…" }`
- `409` логин занят; `400` пароль вне 4..100.

### POST /auth/recover
Новый пароль в окне восстановления (5 минут после админ-сброса).
`{ "username": "...", "password": "новый" }`
- `200` пара токенов; `410` окно истекло; `404` нет пользователя.

### POST /auth/refresh
Обновление токенов. **Публичный** (access не нужен).
`{ "refreshToken": "..." }`
- `200` `{ "status":"OK", "accessToken":"…", "refreshToken":"…" }` (новая пара, старый refresh аннулирован)
- `401` refresh невалиден/просрочен → клиент выполняет logout.

---

## 2. Admin

### POST /admin/reset-password
Сброс пароля пользователя администратором. Требует Bearer + совпадение роли/пароля.
```json
{ "role": "volkov", "password": "<ADMIN_PASSWORD>", "username": "friendLogin" }
```
- `200` `{ "status":"RESET", "message":"Пароль сброшен, окно восстановления 5 минут" }`
- `403` неверная роль или админ-пароль; `404` логин не найден.

`ADMIN_PASSWORD` задаётся переменной окружения сервера (не хардкод).

---

## 3. Heartbeat

### GET /ping  (публичный)
`200` `{ "status":"OK", "timestamp":"2026-06-04T10:00:00Z" }`
Клиент опрашивает каждые 5с; статусы ONLINE (<10с) / DEGRADED (10–20с) / OFFLINE (>20с).

---

## 4. Каталогизация

### POST /scan
Сканирование штрихкода. Пользователь берётся из токена (поле в теле игнорируется).
```json
{ "barcodeValue": "4607038310042" }
```
- `200` `{ "status":"NEW", "draftId":"…" }` — продукта нет, создан черновик.
- `200` `{ "status":"EXISTS", "draftId": null }` — уже в каталоге.

### POST /drafts/{draftId}/photos   (multipart/form-data)
Загрузка фото в черновик. Владелец проверяется по токену.
Поля формы:
- `file` — бинарь изображения (обязательно)
- `photoType` — `BARCODE | FRONT | BACK | INGREDIENTS | NUTRITION | EXTRA`
- `capturedAt` — ISO-8601 (опционально, дата съёмки из EXIF)

Сервер: уменьшает до **Full ≤1920** и делает **Thumbnail ~144px** (JPEG),
дедуплицирует по SHA-256 (ключ `photos/{hash}.jpg`), оригинал не хранит.
- `200` `{ "uploadedCount":1, "requiredCount":4, "missingTypes":["BARCODE","INGREDIENTS","NUTRITION"], "complete":false }`
- `404` черновик не найден; `422` чужой черновик или не-OPEN; `400` пустой файл/неверный тип.

```bash
curl -X POST $API/drafts/$ID/photos -H "Authorization: Bearer $ACCESS" \
     -F "file=@photo.jpg;type=image/jpeg" -F "photoType=FRONT" \
     -F "capturedAt=2026-05-01T12:34:56Z"
```

Обязательны 4 типа: `BARCODE, FRONT, INGREDIENTS, NUTRITION`. `BACK/EXTRA` — опционально.

### POST /drafts/{draftId}/complete
Завершение каталога (владелец по токену; тело не требуется).
- `201` `{ "catalogEntryId":"…", "contributorCompletedCount": 3 }`
- `404` нет черновика; `422` не собраны все обязательные фото (`details` — missingTypes).

### GET /entries/{barcode}
Запись каталога по ШК (любой авторизованный).
- `200`:
```json
{ "id":"…", "barcode":"4607038310042", "contributorId":"…",
  "photos":[ { "id":"…", "type":"FRONT", "storageKey":"photos/<hash>.jpg",
              "capturedAt":"2026-05-01T12:34:56Z" } ],
  "createdAt":"…" }
```
- `404` не найдено.

### GET /photos/{storageKey}?size=thumb|full
Отдаёт изображение (по умолчанию `full`). `size=thumb` — превью ~144px.
`storageKey` — значение из `entries[].photos[].storageKey`.
```bash
curl -H "Authorization: Bearer $ACCESS" "$API/photos/photos/<hash>.jpg?size=thumb" -o thumb.jpg
```

---

## Серверные процессы

- **Очистка мусора** (раз в час): незавершённые черновики (OPEN/ABANDONED) старше 24ч
  удаляются вместе с их фото в MinIO (только если объект больше нигде не используется).
- **Удаление recovery**: аккаунт без пароля, не задавший новый в окне 5 минут, удаляется (раз в минуту).

## Переменные окружения сервера
`DB_URL/DB_USER/DB_PASSWORD`, `MINIO_ENDPOINT/MINIO_ACCESS_KEY/MINIO_SECRET_KEY/MINIO_BUCKET`,
`ADMIN_PASSWORD`, `JWT_SECRET`, `JWT_ACCESS_TTL_HOURS` (24), `JWT_REFRESH_TTL_DAYS` (30),
`CLEANUP_DRAFT_TTL_HOURS` (24).

## Типовой сценарий (curl)
```bash
API=http://localhost:8080/api/v1
# 1) вход/создание
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
