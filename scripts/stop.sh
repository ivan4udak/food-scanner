#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/food-scanner.conf"

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    kill "$PID"
    rm "$PID_FILE"
    echo "Stopped (PID $PID)"
  else
    echo "Process $PID not running. Removing stale PID file."
    rm "$PID_FILE"
  fi
else
  echo "PID file not found. Service may not be running."
fi
