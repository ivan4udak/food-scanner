"""
OCR-сервис (скелет v1.10.1): FastAPI health/metrics + RabbitMQ-консьюмер задач.
Движок пока заглушка (StubEngine → NEEDS_REVIEW). EasyOCR за портом OcrEngine — v1.10.2.

Контракт: см. docs/OCR.md.
  Вход:  очередь ocr.jobs    {jobId, storageKey, photoType, draftId, attempt}
  Выход: очередь ocr.results {jobId, status, rawText, parsedIngredients,
                              parsedNutrition, confidence, errorCode, errorMessage}
"""
import json
import os
import threading
import time

import pika
import uvicorn
from fastapi import FastAPI

RABBIT_URL = os.environ.get("RABBIT_URL", "amqp://guest:guest@rabbitmq:5672/")
JOBS_QUEUE = os.environ.get("OCR_JOBS_QUEUE", "ocr.jobs")
RESULTS_QUEUE = os.environ.get("OCR_RESULTS_QUEUE", "ocr.results")
EXCHANGE = os.environ.get("OCR_EXCHANGE", "ocr")
# backpressure: worker берёт не больше задач, чем может обработать (prefetch=concurrency)
WORKER_CONCURRENCY = max(1, int(os.environ.get("OCR_WORKER_CONCURRENCY", "1")))

app = FastAPI(title="food-scanner-ocr")
_state = {"engine": "stub", "consumed": 0, "broker": False}


@app.get("/health")
def health():
    return {"status": "OK", **_state}


# ── OCR-движок (порт) ────────────────────────────────────────────────
class OcrEngine:
    def recognize(self, image_bytes: bytes) -> dict:
        raise NotImplementedError


class StubEngine(OcrEngine):
    """Заглушка: пока не читаем — помечаем на ручную проверку."""
    def recognize(self, image_bytes: bytes) -> dict:
        return {
            "status": 2,  # NEEDS_REVIEW
            "rawText": None,
            "parsedIngredients": None,
            "parsedNutrition": None,
            "confidence": None,
            "errorCode": None,
            "errorMessage": "stub engine: распознавание не реализовано",
        }


ENGINE: OcrEngine = StubEngine()


def _declare(ch):
    ch.exchange_declare(exchange=EXCHANGE, exchange_type="direct", durable=True)
    ch.queue_declare(queue=JOBS_QUEUE, durable=True)
    ch.queue_declare(queue=RESULTS_QUEUE, durable=True)
    ch.queue_bind(queue=JOBS_QUEUE, exchange=EXCHANGE, routing_key="job")
    ch.queue_bind(queue=RESULTS_QUEUE, exchange=EXCHANGE, routing_key="result")


def _handle(ch, method, _props, body):
    try:
        job = json.loads(body)
        result = ENGINE.recognize(b"")  # фото подтянем из MinIO в v1.10.2
        result["jobId"] = job.get("jobId")
        ch.basic_publish(
            exchange=EXCHANGE, routing_key="result",
            body=json.dumps(result),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )
        _state["consumed"] += 1
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
            print(f"[ocr] consuming {JOBS_QUEUE} (concurrency={WORKER_CONCURRENCY})", flush=True)
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
