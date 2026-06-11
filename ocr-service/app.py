"""
OCR-сервис (v1.11.0): FastAPI health + RabbitMQ-консьюмер + EasyOCR (CPU, raw text).
Скачивает фото из MinIO по storageKey, распознаёт текст, публикует результат в backend.
Парсинг состава/КБЖУ НЕ делаем в этом релизе (только rawText + confidence).

Контракт (docs/OCR.md):
  Вход:  ocr.jobs    {jobId, storageKey, photoType, draftId, attempt}
  Выход: ocr.results {jobId, status, rawText, parsedIngredients, parsedNutrition,
                      confidence, errorCode, errorMessage}
  Статусы v1.11.0: 2 NEEDS_REVIEW (текст есть, парсинга нет) · 3 PHOTO_UNREADABLE · 5 ERROR.
  SUCCESS(4) НЕ ставим за сырой текст — он зарезервирован под распарсенный результат.
"""
import io
import json
import os
import threading
import time
from concurrent.futures import ThreadPoolExecutor, TimeoutError as FuturesTimeout

import pika
import uvicorn
from fastapi import FastAPI

RABBIT_URL = os.environ.get("RABBIT_URL", "amqp://guest:guest@rabbitmq:5672/")
JOBS_QUEUE = os.environ.get("OCR_JOBS_QUEUE", "ocr.jobs")
RESULTS_QUEUE = os.environ.get("OCR_RESULTS_QUEUE", "ocr.results")
EXCHANGE = os.environ.get("OCR_EXCHANGE", "ocr")
# backpressure: worker берёт не больше задач, чем может обработать (prefetch=concurrency)
WORKER_CONCURRENCY = max(1, int(os.environ.get("OCR_WORKER_CONCURRENCY", "1")))
# движок: easyocr (по умолчанию) либо stub (мгновенный откат без пересборки)
ENGINE_NAME = os.environ.get("OCR_ENGINE", "easyocr").lower()
OCR_LANGS = [s.strip() for s in os.environ.get("OCR_LANGS", "ru,en").split(",") if s.strip()]
OCR_JOB_TIMEOUT = float(os.environ.get("OCR_JOB_TIMEOUT_SECONDS", "120"))
DOWNLOAD_TIMEOUT = float(os.environ.get("OCR_IMAGE_DOWNLOAD_TIMEOUT_SECONDS", "30"))
# минимальная средняя уверенность, чтобы считать фото читаемым
OCR_MIN_CONFIDENCE = float(os.environ.get("OCR_MIN_CONFIDENCE", "0.1"))

# MinIO (то же окружение/bucket, что и backend — берём из app.env)
MINIO_ENDPOINT = os.environ.get("MINIO_ENDPOINT", "http://minio:9000")
MINIO_ACCESS_KEY = os.environ.get("MINIO_ACCESS_KEY", "foodscanner")
MINIO_SECRET_KEY = os.environ.get("MINIO_SECRET_KEY", "foodscanner123")
MINIO_BUCKET = os.environ.get("MINIO_BUCKET", "food-images")

app = FastAPI(title="food-scanner-ocr")
_state = {"engine": ENGINE_NAME, "langs": OCR_LANGS, "consumed": 0, "errors": 0, "broker": False}


@app.get("/health")
def health():
    return {"status": "OK", **_state}


def _result(status, raw=None, confidence=None, error_code=None, error_message=None):
    return {
        "status": status, "rawText": raw, "parsedIngredients": None, "parsedNutrition": None,
        "confidence": confidence, "errorCode": error_code, "errorMessage": error_message,
    }


# ── OCR-движок (порт) ────────────────────────────────────────────────
class OcrEngine:
    def recognize(self, image_bytes: bytes) -> dict:
        raise NotImplementedError


class StubOcrEngine(OcrEngine):
    """Заглушка (OCR_ENGINE=stub): rollback/тесты. Текст не распознаётся."""
    def recognize(self, image_bytes: bytes) -> dict:
        return _result(2, error_message="stub engine: распознавание выключено")


class EasyOcrEngine(OcrEngine):
    """EasyOCR (CPU). Lazy-инициализация Reader (модели качаются на первой задаче)."""
    def __init__(self):
        self._reader = None
        self._lock = threading.Lock()

    def _reader_instance(self):
        if self._reader is None:
            with self._lock:
                if self._reader is None:
                    import easyocr  # тяжёлый импорт — только при движке easyocr
                    print(f"[ocr] lazy-loading EasyOCR models langs={OCR_LANGS} (cpu)…", flush=True)
                    self._reader = easyocr.Reader(OCR_LANGS, gpu=False)
                    print("[ocr] EasyOCR ready", flush=True)
        return self._reader

    def recognize(self, image_bytes: bytes) -> dict:
        from PIL import Image
        import numpy as np
        try:
            img = Image.open(io.BytesIO(image_bytes)).convert("L")  # grayscale
        except Exception as e:
            return _result(5, error_code="IMAGE_DECODE_ERROR", error_message=f"decode: {e}")
        detections = self._reader_instance().readtext(np.array(img), detail=1, paragraph=False)
        texts = [d[1] for d in detections if d[1] and str(d[1]).strip()]
        confs = [float(d[2]) for d in detections if len(d) > 2 and d[2] is not None]
        raw = "\n".join(texts).strip()
        avg_conf = round(sum(confs) / len(confs), 4) if confs else None
        if not raw or (avg_conf is not None and avg_conf < OCR_MIN_CONFIDENCE):
            return _result(3, raw="", confidence=avg_conf,
                           error_code="PHOTO_UNREADABLE", error_message="No readable text detected")
        return _result(2, raw=raw, confidence=avg_conf)  # NEEDS_REVIEW: текст есть, парсинга нет


ENGINE: OcrEngine = StubOcrEngine() if ENGINE_NAME == "stub" else EasyOcrEngine()
_executor = ThreadPoolExecutor(max_workers=1)


def _minio_client():
    from minio import Minio
    host = MINIO_ENDPOINT.replace("https://", "").replace("http://", "")
    secure = MINIO_ENDPOINT.startswith("https://")
    return Minio(host, access_key=MINIO_ACCESS_KEY, secret_key=MINIO_SECRET_KEY, secure=secure)


def _fetch(storage_key: str) -> bytes:
    resp = _minio_client().get_object(MINIO_BUCKET, storage_key)
    try:
        return resp.read()
    finally:
        resp.close()
        resp.release_conn()


def _download(storage_key: str):
    """→ (bytes, None) либо (None, error_result). Таймаут на скачивание из MinIO."""
    from minio.error import S3Error
    try:
        data = _executor.submit(_fetch, storage_key).result(timeout=DOWNLOAD_TIMEOUT)
        return data, None
    except FuturesTimeout:
        return None, _result(5, error_code="MINIO_DOWNLOAD_ERROR",
                             error_message=f"download timeout > {DOWNLOAD_TIMEOUT}s")
    except S3Error as e:
        code = "MINIO_OBJECT_NOT_FOUND" if getattr(e, "code", "") == "NoSuchKey" else "MINIO_DOWNLOAD_ERROR"
        return None, _result(5, error_code=code, error_message=str(e))
    except Exception as e:
        return None, _result(5, error_code="MINIO_DOWNLOAD_ERROR", error_message=str(e))


def _process(job: dict) -> dict:
    job_id = job.get("jobId")
    storage_key = job.get("storageKey")
    if not storage_key:
        return _result(5, error_code="NO_STORAGE_KEY", error_message="storageKey отсутствует")
    print(f"[ocr] job {job_id} started (type={job.get('photoType')})", flush=True)

    image_bytes, err = _download(storage_key)
    if err is not None:
        print(f"[ocr] job {job_id} download failed: {err['errorCode']}", flush=True)
        return err
    print(f"[ocr] job {job_id} image downloaded ({len(image_bytes)} bytes)", flush=True)

    t0 = time.monotonic()
    try:
        res = _executor.submit(ENGINE.recognize, image_bytes).result(timeout=OCR_JOB_TIMEOUT)
    except FuturesTimeout:
        print(f"[ocr] job {job_id} timeout > {OCR_JOB_TIMEOUT}s", flush=True)
        return _result(5, error_code="OCR_TIMEOUT", error_message=f"OCR превысил {OCR_JOB_TIMEOUT}s")
    except Exception as e:
        print(f"[ocr] job {job_id} engine error: {e}", flush=True)
        return _result(5, error_code="OCR_ENGINE_ERROR", error_message=str(e))
    dur = round(time.monotonic() - t0, 2)
    raw_len = len(res.get("rawText") or "")
    print(f"[ocr] job {job_id} done status={res['status']} conf={res.get('confidence')} "
          f"textLen={raw_len} in {dur}s", flush=True)
    return res


def _declare(ch):
    ch.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
    ch.queue_declare(queue=JOBS_QUEUE, durable=True)
    ch.queue_declare(queue=RESULTS_QUEUE, durable=True)
    ch.queue_bind(queue=JOBS_QUEUE, exchange=EXCHANGE, routing_key="job")
    ch.queue_bind(queue=RESULTS_QUEUE, exchange=EXCHANGE, routing_key="result")


def _handle(ch, method, _props, body):
    try:
        job = json.loads(body)
        result = _process(job)
        result["jobId"] = job.get("jobId")
        if result.get("status") == 5:
            _state["errors"] += 1
        ch.basic_publish(
            exchange=EXCHANGE, routing_key="result",
            body=json.dumps(result),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )
        _state["consumed"] += 1
    except Exception as e:  # неожиданное — лог, но ack чтобы не зациклить
        print(f"[ocr] handle error: {e}", flush=True)
    finally:
        ch.basic_ack(delivery_tag=method.delivery_tag)


def _consume_loop():
    while True:
        try:
            conn = pika.BlockingConnection(pika.URLParameters(RABBIT_URL))
            ch = conn.channel()
            _declare(ch)
            ch.basic_qos(prefetch_count=WORKER_CONCURRENCY)
            ch.basic_consume(queue=JOBS_QUEUE, on_message_callback=_handle)
            _state["broker"] = True
            print(f"[ocr] consuming {JOBS_QUEUE} (engine={ENGINE_NAME}, langs={OCR_LANGS}, "
                  f"concurrency={WORKER_CONCURRENCY}, prefetch={WORKER_CONCURRENCY})", flush=True)
            ch.start_consuming()
        except Exception as e:  # брокер недоступен — ретрай
            _state["broker"] = False
            print(f"[ocr] broker error: {e}; retry in 5s", flush=True)
            time.sleep(5)


@app.on_event("startup")
def _startup():
    threading.Thread(target=_consume_loop, daemon=True).start()


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8090)
