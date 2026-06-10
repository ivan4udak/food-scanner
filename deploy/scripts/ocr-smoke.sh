#!/usr/bin/env bash
# OCR smoke-check (staging): контейнеры, очередь, статусы ocr_jobs.
# Запуск НА СЕРВЕРЕ: deploy/scripts/ocr-smoke.sh [staging]
# Не трогает stable/production.
set -uo pipefail

ENV="${1:-staging}"
[[ "$ENV" == "staging" ]] || { echo "Только staging. Дано: $ENV"; exit 1; }

ok() { echo "  ✅ $*"; }
no() { echo "  ❌ $*"; }

echo "== OCR smoke ($ENV) =="

echo "[1] контейнеры"
for c in rabbitmq-$ENV ocr-$ENV backend-$ENV; do
  if docker ps --format '{{.Names}}' | grep -qx "$c"; then ok "$c up"; else no "$c НЕ запущен"; fi
done

echo "[2] rabbitmq ping"
docker exec "rabbitmq-$ENV" rabbitmq-diagnostics -q ping >/dev/null 2>&1 && ok "rabbitmq отвечает" || no "rabbitmq не отвечает"

echo "[3] ocr-service consuming"
docker logs "ocr-$ENV" 2>&1 | grep -q "consuming" && ok "ocr-service слушает очередь" || no "нет 'consuming' в логах ocr-$ENV"

echo "[4] backend AMQP без фатала"
docker logs "backend-$ENV" 2>&1 | grep -iq "AmqpConnectException\|Connection refused.*5672" && no "backend: проблемы с брокером" || ok "backend: брокер ок (или OCR off)"

echo "[5] статусы ocr_jobs"
docker exec "postgres-$ENV" psql -U "${POSTGRES_USER:-foodscanner}" -d "${POSTGRES_DB:-foodscanner}" -At \
  -c "SELECT status, count(*) FROM food_catalog.ocr_jobs GROUP BY status ORDER BY status;" 2>/dev/null \
  | sed 's/^/  status=/' || no "не удалось прочитать ocr_jobs (POSTGRES_USER/DB?)"

echo "Готово. Подробности — docs/OCR_ROLLOUT.md"
