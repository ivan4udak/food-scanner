#!/usr/bin/env bash
# Ждёт готовности backend-контейнера: либо Docker healthcheck = healthy,
# либо прямой ответ /api/v1/ping. Возвращает 0 при успехе, 1 при таймауте.
# Usage: health-check.sh <container> [retries=40] [sleep=3]
set -Eeuo pipefail

CONTAINER="${1:?usage: health-check.sh <container> [retries] [sleep]}"
RETRIES="${2:-40}"
SLEEP="${3:-3}"

for i in $(seq 1 "$RETRIES"); do
  # 1) статус docker healthcheck (если определён)
  status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$CONTAINER" 2>/dev/null || echo missing)"
  if [[ "$status" == "healthy" ]]; then
    echo "healthy (docker): $CONTAINER (попытка $i)"
    exit 0
  fi
  # 2) прямой ping внутри контейнера
  if docker exec "$CONTAINER" wget -qO- http://127.0.0.1:8080/api/v1/ping >/dev/null 2>&1; then
    echo "healthy (ping): $CONTAINER (попытка $i)"
    exit 0
  fi
  sleep "$SLEEP"
done

echo "UNHEALTHY: $CONTAINER не поднялся за $((RETRIES * SLEEP))с" >&2
exit 1
