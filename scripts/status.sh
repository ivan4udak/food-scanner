#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/food-scanner.conf"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    echo "✅ RUNNING (PID $PID, port $SERVER_PORT)"
    echo "   http://localhost:$SERVER_PORT/api/v1"
    echo "   Health: http://localhost:$SERVER_PORT/actuator/health"
  else
    echo "⚠️  STALE PID file (PID $PID not found)"
  fi
else
  OS="$(uname -s)"
  if [ "$OS" = "Darwin" ]; then
    launchctl list | grep foodscanner && echo "✅ Running via launchd" || echo "❌ STOPPED"
  elif [ "$OS" = "Linux" ]; then
    systemctl is-active food-scanner && echo "✅ Running via systemd" || echo "❌ STOPPED"
  fi
fi
