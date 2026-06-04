#!/usr/bin/env bash
# Отправка сообщения в Telegram. Используется как сервером, так и (в основном) CI.
# Требует TELEGRAM_BOT_TOKEN и TELEGRAM_CHAT_ID в окружении.
set -Eeuo pipefail

MSG="${1:?usage: notify-telegram.sh <message>}"
: "${TELEGRAM_BOT_TOKEN:?TELEGRAM_BOT_TOKEN not set}"
: "${TELEGRAM_CHAT_ID:?TELEGRAM_CHAT_ID not set}"

curl -fsS -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage" \
  -d chat_id="${TELEGRAM_CHAT_ID}" \
  -d parse_mode="HTML" \
  -d disable_web_page_preview="true" \
  --data-urlencode text="${MSG}" >/dev/null
