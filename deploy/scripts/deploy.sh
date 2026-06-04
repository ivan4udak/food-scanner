#!/usr/bin/env bash
# Деплой окружения. Для production делегирует blue-green.
# Вызывается из GitHub Actions по SSH с экспортированными BACKEND_IMAGE/WEB_IMAGE/GIT_SHA.
#
# Usage: deploy.sh <staging|preprod|production>
set -Eeuo pipefail

ENVIRONMENT="${1:?usage: deploy.sh <staging|preprod|production>}"
BASE="${FS_BASE:-/opt/foodscanner}"
ENV_DIR="$BASE/$ENVIRONMENT"
SCRIPTS="$BASE/scripts"
source "$SCRIPTS/lib.sh"

: "${BACKEND_IMAGE:?BACKEND_IMAGE not set}"
: "${WEB_IMAGE:?WEB_IMAGE not set}"
: "${GIT_SHA:=unknown}"
[[ -d "$ENV_DIR" ]] || die "нет каталога окружения: $ENV_DIR"
[[ -f "$ENV_DIR/app.env" ]] || die "нет $ENV_DIR/app.env (секреты)"

cd "$ENV_DIR"
export BACKEND_IMAGE WEB_IMAGE

# production → blue-green с health-gate и авто-rollback
if [[ "$ENVIRONMENT" == "production" ]]; then
  exec "$SCRIPTS/blue-green-deploy.sh"
fi

log "Деплой $ENVIRONMENT: backend=$BACKEND_IMAGE web=$WEB_IMAGE (sha=$GIT_SHA)"

# Запоминаем предыдущие образы для отката
PREV_LINE="$(last_release "$ENV_DIR")"
PREV_BACKEND="$(release_field "$PREV_LINE" 3 2>/dev/null || true)"
PREV_WEB="$(release_field "$PREV_LINE" 4 2>/dev/null || true)"

docker compose pull
docker compose up -d --remove-orphans

if "$SCRIPTS/health-check.sh" "backend-$ENVIRONMENT" 40 3; then
  record_release "$ENV_DIR" "$GIT_SHA" "$BACKEND_IMAGE" "$WEB_IMAGE"
  log "✅ $ENVIRONMENT обновлён до $GIT_SHA"
  notify_ok "$ENVIRONMENT" "$GIT_SHA"
else
  log "❌ health-check не прошёл — откат"
  if [[ -n "$PREV_BACKEND" ]]; then
    log "Откат к backend=$PREV_BACKEND web=$PREV_WEB"
    BACKEND_IMAGE="$PREV_BACKEND" WEB_IMAGE="${PREV_WEB:-$WEB_IMAGE}" docker compose up -d --remove-orphans
    "$SCRIPTS/health-check.sh" "backend-$ENVIRONMENT" 20 3 || log "⚠ предыдущая версия тоже нездорова"
  else
    log "⚠ нет предыдущего релиза для отката"
  fi
  notify_fail "$ENVIRONMENT" "$GIT_SHA"
  exit 1
fi
