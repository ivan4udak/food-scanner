#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════════
#  Food Scanner — установка и регистрация фонового сервиса
#
#  Использование:
#    chmod +x scripts/install.sh
#    ./scripts/install.sh
#
#  Поддерживает macOS (launchd) и Linux (systemd).
# ═══════════════════════════════════════════════════════════════════
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONF="$SCRIPT_DIR/food-scanner.conf"

# Цвета
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

echo ""
echo "╔══════════════════════════════════════╗"
echo "║   Food Scanner — Install Script      ║"
echo "╚══════════════════════════════════════╝"
echo ""

# ── 1. Проверки ──────────────────────────────────────────────────
info "Checking prerequisites..."

command -v java >/dev/null 2>&1 || error "Java not found. Install Java 21+."
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
[ "$JAVA_VER" -ge 21 ] 2>/dev/null || warn "Java 21+ recommended. Found: $JAVA_VER"

command -v mvn >/dev/null 2>&1 || error "Maven not found."
info "Java $(java -version 2>&1 | head -1) ✓"
info "Maven $(mvn -version 2>&1 | head -1) ✓"

# ── 2. Сборка ────────────────────────────────────────────────────
info "Building JAR (skipping tests)..."
cd "$PROJECT_DIR"
mvn package -DskipTests -q
info "Build complete ✓"

# ── 3. Создать папку для логов ───────────────────────────────────
mkdir -p "$PROJECT_DIR/logs"
info "Log directory: $PROJECT_DIR/logs ✓"

# ── 4. Установка ─────────────────────────────────────────────────
OS="$(uname -s)"

if [ "$OS" = "Darwin" ]; then
    # ── macOS: launchd ────────────────────────────────────────────
    info "Detected macOS — installing as launchd agent..."

    source "$CONF"
    PLIST="$HOME/Library/LaunchAgents/com.foodscanner.app.plist"

    cat > "$PLIST" << PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
  "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>             <string>com.foodscanner.app</string>
    <key>ProgramArguments</key>
    <array>
        <string>$(which java)</string>
        <string>-Xms256m</string>
        <string>-Xmx512m</string>
        <string>-XX:+UseG1GC</string>
        <string>-Dserver.port=${SERVER_PORT}</string>
        <string>-Dspring.profiles.active=${SPRING_PROFILE}</string>
        <string>-DDB_URL=${DB_URL}</string>
        <string>-DDB_USER=${DB_USER}</string>
        <string>-DDB_PASSWORD=${DB_PASSWORD}</string>
        <string>-DLOG_LEVEL_ROOT=${LOG_LEVEL_ROOT}</string>
        <string>-DLOG_LEVEL_APP=${LOG_LEVEL_APP}</string>
        <string>-DLOG_LEVEL_WEB=${LOG_LEVEL_WEB}</string>
        <string>-DLOG_FILE=${PROJECT_DIR}/${LOG_FILE}</string>
        <string>-jar</string>
        <string>${PROJECT_DIR}/${JAR_PATH}</string>
    </array>
    <key>WorkingDirectory</key>  <string>${PROJECT_DIR}</string>
    <key>RunAtLoad</key>         <true/>
    <key>KeepAlive</key>         <true/>
    <key>StandardOutPath</key>   <string>${PROJECT_DIR}/logs/stdout.log</string>
    <key>StandardErrorPath</key> <string>${PROJECT_DIR}/logs/stderr.log</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>SPRING_PROFILES_ACTIVE</key> <string>${SPRING_PROFILE}</string>
    </dict>
</dict>
</plist>
PLIST

    launchctl unload "$PLIST" 2>/dev/null || true
    launchctl load -w "$PLIST"

    info "Service installed as launchd agent ✓"
    info "Plist: $PLIST"
    info ""
    info "Commands:"
    info "  Start:   launchctl start com.foodscanner.app"
    info "  Stop:    launchctl stop com.foodscanner.app"
    info "  Status:  launchctl list | grep foodscanner"
    info "  Logs:    tail -f $PROJECT_DIR/logs/stdout.log"
    info "  Remove:  launchctl unload $PLIST && rm $PLIST"

elif [ "$OS" = "Linux" ]; then
    # ── Linux: systemd ────────────────────────────────────────────
    info "Detected Linux — installing as systemd service..."

    source "$CONF"
    SERVICE_FILE="/etc/systemd/system/food-scanner.service"
    CURRENT_USER="$(whoami)"

    sudo tee "$SERVICE_FILE" > /dev/null << UNIT
[Unit]
Description=Food Scanner Application
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=${CURRENT_USER}
WorkingDirectory=${PROJECT_DIR}
ExecStart=$(which java) \
    ${JAVA_OPTS} \
    -Dserver.port=${SERVER_PORT} \
    -Dspring.profiles.active=${SPRING_PROFILE} \
    -DDB_URL=${DB_URL} \
    -DDB_USER=${DB_USER} \
    -DDB_PASSWORD=${DB_PASSWORD} \
    -DLOG_LEVEL_ROOT=${LOG_LEVEL_ROOT} \
    -DLOG_LEVEL_APP=${LOG_LEVEL_APP} \
    -DLOG_LEVEL_WEB=${LOG_LEVEL_WEB} \
    -DLOG_FILE=${PROJECT_DIR}/${LOG_FILE} \
    -jar ${PROJECT_DIR}/${JAR_PATH}

Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=food-scanner

# Безопасность
NoNewPrivileges=yes
PrivateTmp=yes

[Install]
WantedBy=multi-user.target
UNIT

    sudo systemctl daemon-reload
    sudo systemctl enable food-scanner
    sudo systemctl start food-scanner

    info "Service installed as systemd unit ✓"
    info ""
    info "Commands:"
    info "  Status:  sudo systemctl status food-scanner"
    info "  Stop:    sudo systemctl stop food-scanner"
    info "  Restart: sudo systemctl restart food-scanner"
    info "  Logs:    sudo journalctl -u food-scanner -f"
    info "  Remove:  sudo systemctl disable --now food-scanner && sudo rm $SERVICE_FILE"

else
    error "Unsupported OS: $OS (supported: Darwin, Linux)"
fi

echo ""
info "Food Scanner is running at http://localhost:${SERVER_PORT:-8080}"
info "API: http://localhost:${SERVER_PORT:-8080}/api/v1"
echo ""
