#!/usr/bin/env bash
# Быстрый откат окружения к предыдущему (или указанному) релизу из истории.
# Образы уже в локальном кэше/GHCR, поэтому откат почти мгновенный.
# Usage:
#   rollback.sh <env>            # к предыдущему релизу
#   rollback.sh <env> <git_sha>  # к конкретному SHA из releases.log
set -Eeuo pipefail

ENVIRONMENT="${1:?usage: rollback.sh <env> [git_sha]}"
BASE="${FS_BASE:-/opt/foodscanner}"
ENV_DIR="$BASE/$ENVIRONMENT"
SCRIPTS="$BASE/scripts"
source "$SCRIPTS/lib.sh"
[[ -d "$ENV_DIR" ]] || die "нет каталога окружения: $ENV_DIR"

TARGET_SHA="${2:-}"
if [[ -n "$TARGET_SHA" ]]; then
  LINE="$(grep "|$TARGET_SHA|" "$ENV_DIR/releases.log" 2>/dev/null | tail -n 1 || true)"
  [[ -n "$LINE" ]] || die "SHA $TARGET_SHA не найден в $ENV_DIR/releases.log"
else
  LINE="$(prev_release "$ENV_DIR")"
  [[ -n "$LINE" ]] || die "нет предыдущего релиза в истории"
fi

BACKEND_IMAGE="$(release_field "$LINE" 3)"
WEB_IMAGE="$(release_field "$LINE" 4)"
GIT_SHA="$(release_field "$LINE" 2)"
export BACKEND_IMAGE WEB_IMAGE GIT_SHA

log "ОТКАТ $ENVIRONMENT → sha=$GIT_SHA"
log "  backend=$BACKEND_IMAGE"
log "  web=$WEB_IMAGE"

# Переиспользуем обычный путь деплоя (in-place с health-check).
exec "$SCRIPTS/deploy.sh" "$ENVIRONMENT"
