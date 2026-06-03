#!/usr/bin/env bash
# Запуск без установки (foreground или background)
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
source "$SCRIPT_DIR/food-scanner.conf"

JAR="$PROJECT_DIR/$JAR_PATH"
[ -f "$JAR" ] || { echo "JAR not found. Run: mvn package -DskipTests"; exit 1; }

mkdir -p "$PROJECT_DIR/logs"

CMD=(
  java $JAVA_OPTS
  -Dserver.port="$SERVER_PORT"
  -Dspring.profiles.active="$SPRING_PROFILE"
  -DDB_URL="$DB_URL"
  -DDB_USER="$DB_USER"
  -DDB_PASSWORD="$DB_PASSWORD"
  -DLOG_LEVEL_ROOT="$LOG_LEVEL_ROOT"
  -DLOG_LEVEL_APP="$LOG_LEVEL_APP"
  -DLOG_LEVEL_WEB="$LOG_LEVEL_WEB"
  -DLOG_FILE="$PROJECT_DIR/$LOG_FILE"
  -jar "$JAR"
)

if [ "${1:-}" = "-d" ]; then
  echo "Starting in background (PID file: $PID_FILE)..."
  "${CMD[@]}" > "$PROJECT_DIR/logs/stdout.log" 2>&1 &
  echo $! > "$PID_FILE"
  echo "Started. PID: $(cat $PID_FILE)"
  echo "Logs: tail -f $PROJECT_DIR/logs/stdout.log"
else
  echo "Starting in foreground (Ctrl+C to stop)..."
  "${CMD[@]}"
fi
