#!/usr/bin/env bash
# Общие функции для deploy/rollback/blue-green скриптов.
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

# Telegram-уведомление (no-op, если секреты не заданы — основной путь уведомлений в CI).
notify() {
  [[ -n "${TELEGRAM_BOT_TOKEN:-}" && -n "${TELEGRAM_CHAT_ID:-}" ]] || return 0
  "$SCRIPTS/notify-telegram.sh" "$1" || true
}
notify_ok()   { notify "✅ Deploy OK — <b>$1</b> ($2) на $(hostname)"; }
notify_fail() { notify "❌ Deploy FAILED — <b>$1</b> ($2) на $(hostname)"; }

# Чистка диска: удаляет неиспользуемые образы старше 48ч (маленький диск сервера).
# Running-образы и недавние (для быстрого отката) сохраняются.
prune_images() { docker image prune -af --filter "until=48h" >/dev/null 2>&1 || true; }
