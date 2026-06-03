#!/usr/bin/env bash
# Удобный просмотр логов
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
source "$SCRIPT_DIR/food-scanner.conf"

MODE="${1:-tail}"

case "$MODE" in
  tail)
    echo "Tailing logs (Ctrl+C to stop)..."
    tail -f "$PROJECT_DIR/$LOG_FILE" 2>/dev/null \
      || tail -f "$PROJECT_DIR/logs/stdout.log" 2>/dev/null \
      || echo "No log file found. App may be logging to stdout only."
    ;;
  errors)
    echo "=== ERROR lines ==="
    grep -E "ERROR|WARN" "$PROJECT_DIR/$LOG_FILE" 2>/dev/null | tail -50 \
      || echo "No log file."
    ;;
  http)
    echo "=== HTTP requests ==="
    grep -E "GET |POST |PUT |DELETE |PATCH " "$PROJECT_DIR/$LOG_FILE" 2>/dev/null | tail -50 \
      || echo "No log file."
    ;;
  sql)
    echo "=== SQL queries ==="
    grep -E "select|insert|update|delete|Hibernate:" "$PROJECT_DIR/$LOG_FILE" 2>/dev/null | tail -50 \
      || echo "No log file."
    ;;
  *)
    echo "Usage: $0 [tail|errors|http|sql]"
    ;;
esac
