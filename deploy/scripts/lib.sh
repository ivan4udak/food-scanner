#!/usr/bin/env bash
# Общие функции для deploy/rollback скриптов.
# Подключается через: source /opt/foodscanner/scripts/lib.sh

BASE="${FS_BASE:-/opt/foodscanner}"
SCRIPTS="$BASE/scripts"
MAX_HISTORY="${FS_MAX_HISTORY:-10}"

log() { echo "[$(date -u +%FT%TZ)] $*"; }
die() { log "ERROR: $*" >&2; exit 1; }

# Записать релиз в историю: releases.log = ts|sha|backend_image|web_image|color
record_release() {
  local dir="$1" sha="$2" backend="$3" web="$4" color="${5:-}"
  echo "$(date -u +%FT%TZ)|$sha|$backend|$web|$color" >> "$dir/releases.log"
  tail -n "$MAX_HISTORY" "$dir/releases.log" > "$dir/releases.log.tmp" 2>/dev/null || true
  mv -f "$dir/releases.log.tmp" "$dir/releases.log" 2>/dev/null || true
}

# Поле N (1=ts,2=sha,3=backend,4=web,5=color) из строки релиза.
release_field() { echo "$1" | cut -d'|' -f"$2"; }

# Последняя (текущая) строка истории или пусто.
last_release() { [[ -f "$1/releases.log" ]] && tail -n 1 "$1/releases.log" || true; }

# Предыдущая строка (для отката) или пусто.
prev_release() { [[ -f "$1/releases.log" ]] && tail -n 2 "$1/releases.log" | head -n 1 || true; }

# ── Карта окружение → публичный адрес (единственный источник истины) ──
# Используется и серверными скриптами, и CI-джобой notify (она делает checkout).
service_url_for_env() {
  case "$1" in
    staging)    echo "https://foodscanner-staging.duckdns.org" ;;
    stable)     echo "https://foodscanner-preprod.duckdns.org" ;;
    production) echo "https://foodscanner.duckdns.org" ;;
    *)          echo "" ;;
  esac
}
api_url_for_env() {
  local u; u="$(service_url_for_env "$1")"
  [[ -n "$u" ]] && echo "${u}/api/v1" || echo ""
}
branch_for_env() {
  case "$1" in
    staging) echo "test" ;; stable) echo "main" ;; production) echo "release" ;; *) echo "$1" ;;
  esac
}

# Telegram-уведомление (no-op, если секреты не заданы — основной путь уведомлений в CI).
notify() {
  [[ -n "${TELEGRAM_BOT_TOKEN:-}" && -n "${TELEGRAM_CHAT_ID:-}" ]] || return 0
  "$SCRIPTS/notify-telegram.sh" "$1" || true
}

# Шапка сообщения: окружение/ветка/версия/коммит/Service/API.
_deploy_header() {
  local env="$1" sha="$2"
  printf 'Environment: %s\nBranch: %s\nVersion: %s\nCommit: %s\n\nService: %s\nAPI: %s' \
    "$env" "$(branch_for_env "$env")" "${VERSION_LABEL:-n/a}" "${sha:0:7}" \
    "$(service_url_for_env "$env")" "$(api_url_for_env "$env")"
}
notify_ok() {
  notify "$(printf '✅ Food Scanner deployed\n\n%s\n\nStatus: healthy' "$(_deploy_header "$1" "$2")")"
}
notify_fail() {
  notify "$(printf '❌ Food Scanner deploy failed\n\n%s\n\nFailed step: health-check\nRollback: started' \
    "$(_deploy_header "$1" "$2")")"
}
notify_rollback_ok() {
  # $1=env $2=prev_sha
  notify "$(printf '↩️ Food Scanner rollback completed\n\nEnvironment: %s\nBranch: %s\nCurrent service: %s\nRolled back to: %s\n\nStatus: healthy' \
    "$1" "$(branch_for_env "$1")" "$(service_url_for_env "$1")" "${2:0:7}")"
}
notify_rollback_fail() {
  notify "$(printf '🚨 Food Scanner rollback failed\n\nEnvironment: %s\nBranch: %s\nService: %s\n\nManual intervention required.' \
    "$1" "$(branch_for_env "$1")" "$(service_url_for_env "$1")")"
}

# Чистка диска: удаляет неиспользуемые образы старше 48ч (маленький диск сервера).
# Running-образы и недавние (для быстрого отката) сохраняются.
prune_images() { docker image prune -af --filter "until=48h" >/dev/null 2>&1 || true; }

# Свободно ГБ на корне (целое, floor).
free_disk_gb() { df -P / | awk 'NR==2 {printf "%d", $4/1024/1024}'; }

# Гигиена диска ПЕРЕД pull тяжёлых образов (OCR/torch ~2-4ГБ/SHA быстро забивают 30ГБ → backend
# crash-loop «No space left on device»). Безопасно: НЕ трогаем volumes (Postgres/MinIO) и running-образы.
# При нехватке после очистки — прерываем деплой ДО старта (не оставляем частичный деплой).
ensure_disk_space() {
  local min="${MIN_FREE_DISK_GB:-5}"
  log "Диск до очистки: $(df -h / | awk 'NR==2 {print $4" свободно из "$2" ("$5")"}')"
  docker system df 2>/dev/null | sed 's/^/  /' || true
  if [[ "$(free_disk_gb)" -lt "$min" ]]; then
    log "Свободно <${min}ГБ — безопасная очистка неиспользуемых образов/кэша/контейнеров (volumes НЕ трогаем)…"
    docker image prune -af      >/dev/null 2>&1 || true
    docker builder prune -af    >/dev/null 2>&1 || true
    docker container prune -f   >/dev/null 2>&1 || true
    log "Диск после очистки: $(df -h / | awk 'NR==2 {print $4" свободно из "$2" ("$5")"}')"
  fi
  local avail; avail="$(free_disk_gb)"
  [[ "$avail" -ge "$min" ]] || die "недостаточно места: свободно ${avail}ГБ < ${min}ГБ — деплой прерван до старта (не оставляем частичный деплой). Увеличьте диск или почистите вручную."
}
