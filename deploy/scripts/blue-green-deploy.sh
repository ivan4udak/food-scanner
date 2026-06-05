#!/usr/bin/env bash
# Blue-Green деплой production без Kubernetes.
# Поднимает ПРОСТАИВАЮЩИЙ цвет с новым образом → health-check → переключение Caddy
# → остановка старого. При неудаче health-check новый цвет удаляется, трафик
# остаётся на текущем (автоматический rollback). Требует экспортированных
# BACKEND_IMAGE/WEB_IMAGE/GIT_SHA. Запускается из deploy.sh.
set -Eeuo pipefail

BASE="${FS_BASE:-/opt/foodscanner}"
ENV_DIR="$BASE/production"
SCRIPTS="$BASE/scripts"
source "$SCRIPTS/lib.sh"

: "${BACKEND_IMAGE:?}"; : "${WEB_IMAGE:?}"; : "${GIT_SHA:=unknown}"
cd "$ENV_DIR"
export BACKEND_IMAGE WEB_IMAGE

ACTIVE="$(cat "$ENV_DIR/active_color" 2>/dev/null || echo blue)"
if [[ "$ACTIVE" == "blue" ]]; then IDLE="green"; else IDLE="blue"; fi
log "production: активный=$ACTIVE, деплой в простаивающий=$IDLE ($BACKEND_IMAGE)"

# Общие сервисы и статика всегда подняты.
docker compose pull
docker compose up -d --remove-orphans postgres minio web

# Поднимаем простаивающий цвет с новым образом.
docker compose --profile "$IDLE" up -d "backend-$IDLE"

if "$SCRIPTS/health-check.sh" "backend-$IDLE" 40 3; then
  log "✅ $IDLE здоров — переключаем трафик"
  "$SCRIPTS/blue-green-switch.sh" "$IDLE"
  echo "$IDLE" > "$ENV_DIR/active_color"
  record_release "$ENV_DIR" "$GIT_SHA" "$BACKEND_IMAGE" "$WEB_IMAGE" "$IDLE"

  # Гасим старый цвет (с задержкой на дренаж соединений).
  sleep 5
  docker compose --profile "$ACTIVE" rm -sf "backend-$ACTIVE" 2>/dev/null || true
  prune_images   # освободить диск от старых образов
  log "🟢 production на $IDLE ($GIT_SHA), старый $ACTIVE остановлен"
  notify_ok "production" "$GIT_SHA → $IDLE"
else
  log "❌ $IDLE не прошёл health-check — rollback (трафик остаётся на $ACTIVE)"
  docker compose --profile "$IDLE" rm -sf "backend-$IDLE" 2>/dev/null || true
  notify_fail "production" "$GIT_SHA ($IDLE не стартовал)"
  exit 1
fi
