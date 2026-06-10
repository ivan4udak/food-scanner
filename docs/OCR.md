# OCR — контракт микросервиса (v1.10)

Стек: **RabbitMQ** (очередь) + **OCR-сервис** (Python/FastAPI, движок EasyOCR за портом
`OcrEngine`), на том же сервере, низкий объём. Реализуется поэтапно (скелет → движок).

## Статусы задачи (0–5)
| code | status | смысл |
|---|---|---|
| 0 | QUEUED | поставлена в очередь |
| 1 | IN_PROGRESS_READABLE | взята в работу, фото пригодно для чтения |
| 2 | NEEDS_REVIEW | распознано частично/неоднозначно — проверка админом |
| 3 | PHOTO_UNREADABLE | нечитаемо (размыто/обрезано/засвет/нет текста) |
| 4 | SUCCESS | распознано и распарсено |
| 5 | ERROR | прочая ошибка (хранить error_code + error_message) |

## Жизненный цикл
1. Backend при загрузке фото `INGREDIENTS`/`NUTRITION` создаёт `ocr_jobs` (QUEUED) и
   публикует задачу в RabbitMQ.
2. OCR-сервис потребляет задачу → тянет фото из MinIO по `storage_key` → движок →
   парсит состав/КБЖУ → пишет результат и статус.
3. Backend применяет результат к `ocr_jobs`; статусы видны в админке/клиенте.
4. Retry: при ERROR/таймауте — повтор до N раз (attempts), затем dead-letter.

## RabbitMQ-топология (план следующего среза)
- exchange `ocr` (direct), очередь `ocr.jobs` (routing `job`), `ocr.results` (routing `result`),
  dead-letter `ocr.jobs.dlq`. Ack после успешной обработки; nack→retry/DLQ.

### Сообщение задачи (job)
```json
{ "jobId":"uuid", "storageKey":"photos/<hash>.jpg", "photoType":"INGREDIENTS",
  "draftId":"uuid", "attempt":1 }
```
### Сообщение результата (result)
```json
{ "jobId":"uuid", "status":4, "rawText":"…", "parsedIngredients":"…",
  "parsedNutrition":{ "kcal":250, "protein":10, "fat":12, "carb":30 },
  "confidence":0.82, "errorCode":null, "errorMessage":null }
```

## Хранение (`ocr_jobs`)
`id, draft_id, catalog_entry_id, storage_key, photo_type, status(smallint), attempts,
raw_text, parsed_ingredients, parsed_nutrition(jsonb), confidence, error_code,
error_message, created_at, updated_at`.

## DTO с nullable OCR-полями
`ocrStatus` уже зарезервирован nullable в `/me/scans` (в UI не показывается, пока пусто).

## Этапы
- **v1.10.0 (этот срез):** контракт + `ocr_jobs` + domain `OcrStatus`/`OcrJob` + репозиторий +
  enqueue (создание QUEUED при загрузке INGREDIENTS/NUTRITION). Без брокера/движка.
- **v1.10.1 (готово):** RabbitMQ-контракт + ocr-service (FastAPI, заглушка движка) + backend AMQP под флагом. Активация — отдельный rollout.
- **v1.10.2+:** движок EasyOCR за портом, парсинг состава/КБЖУ, retry/DLQ, отображение статусов.
