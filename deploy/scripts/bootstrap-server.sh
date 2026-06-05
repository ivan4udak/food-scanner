#!/usr/bin/env bash
# Однократная подготовка чистого Ubuntu Server 24.04 (запускать с sudo).
# Ставит Docker, создаёт каталоги/сеть, файрвол, logrotate, systemd-юниты.
# Идемпотентен — можно запускать повторно.
set -Eeuo pipefail

BASE="/opt/foodscanner"
REPO_DEPLOY="${1:-}"   # путь к каталогу deploy/ из репозитория (для копирования конфигов)

echo "==> 1. Docker"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

echo "==> 2. Каталоги $BASE"
mkdir -p "$BASE"/{scripts,caddy,monitoring,staging,production}
touch "$BASE"/{staging,production}/releases.log

echo "==> 3. Внешняя docker-сеть foodscanner-edge"
docker network inspect foodscanner-edge >/dev/null 2>&1 || docker network create foodscanner-edge

echo "==> 4. Файрвол (SSH + 80/443)"
if command -v ufw >/dev/null 2>&1; then
  ufw allow OpenSSH || true
  ufw allow 80/tcp || true
  ufw allow 443/tcp || true
  yes | ufw enable || true
fi

if [[ -n "$REPO_DEPLOY" && -d "$REPO_DEPLOY" ]]; then
  echo "==> 5. Копирование конфигов из $REPO_DEPLOY"
  cp -f "$REPO_DEPLOY"/scripts/*.sh "$BASE/scripts/"; chmod +x "$BASE/scripts/"*.sh
  cp -f "$REPO_DEPLOY"/compose/docker-compose.staging.yml    "$BASE/staging/docker-compose.yml"
  cp -f "$REPO_DEPLOY"/compose/docker-compose.production.yml "$BASE/production/docker-compose.yml"
  cp -f "$REPO_DEPLOY"/compose/docker-compose.caddy.yml      "$BASE/caddy/docker-compose.yml"
  cp -f "$REPO_DEPLOY"/compose/docker-compose.monitoring.yml "$BASE/monitoring/docker-compose.yml"
  cp -f "$REPO_DEPLOY"/caddy/Caddyfile                       "$BASE/caddy/Caddyfile"
  cp -rf "$REPO_DEPLOY"/monitoring/*                         "$BASE/monitoring/"
  # systemd + logrotate
  cp -f "$REPO_DEPLOY"/systemd/*.service /etc/systemd/system/
  cp -f "$REPO_DEPLOY"/logrotate/foodscanner /etc/logrotate.d/foodscanner
  systemctl daemon-reload
fi

cat <<EOF

Готово. Осталось вручную (секреты НЕ в репозитории):
  1) Создать app.env в каждом каталоге из *.env.*.example и chmod 600:
       $BASE/staging/app.env  $BASE/production/app.env
       $BASE/caddy/app.env    $BASE/monitoring/app.env
  2) В DuckDNS создать поддомены (staging./корень/monitoring.) на IP сервера.
  3) Поднять edge-сервисы:
       systemctl enable --now foodscanner-caddy foodscanner-monitoring
  4) Деплой — автоматически из GitHub Actions (push в test/release).
EOF
