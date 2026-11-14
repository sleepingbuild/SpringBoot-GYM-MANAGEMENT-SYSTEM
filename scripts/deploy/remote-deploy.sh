#!/usr/bin/env bash
# remote-deploy.sh - CHAY TREN MAY CHU (Jenkins goi qua SSH, xem Jenkinsfile stage "Deploy SSH").
# Lay ma nguon dung commit vua xanh, dung lai backend + frontend + Prometheus + Grafana bang Docker Compose,
# roi cho den khi tat ca tra loi. Giu nguyen SQL Server, Redis va du lieu.
#
# Bien moi truong (Jenkins truyen vao):
#   GMS_REPO_URL      (bat buoc) dia chi git cua repo
#   GMS_COMMIT        commit can trien khai (mac dinh: dau nhanh GMS_BRANCH)
#   GMS_BRANCH        nhanh (mac dinh master)
#   GMS_PUBLIC_HOST   dia chi trinh duyet dung de goi API (IP hoac ten may chu), mac dinh localhost
#   GMS_APP_DIR       thu muc dat ma nguon tren may chu (mac dinh $HOME/gms)
# Bi mat (mat khau DB, JWT, Grafana) dat trong ~/.gms.env tren may chu, KHONG dat trong repo. Xem docs/CICD_SSH_DEPLOY.md.
set -euo pipefail

REPO_URL="${GMS_REPO_URL:?Thieu GMS_REPO_URL}"
BRANCH="${GMS_BRANCH:-master}"
COMMIT="${GMS_COMMIT:-}"
APP_DIR="${GMS_APP_DIR:-$HOME/gms}"
PUBLIC_HOST="${GMS_PUBLIC_HOST:-localhost}"
API_PORT="${GMS_API_PORT:-8082}"
WEB_PORT="${GMS_WEB_PORT:-3000}"
PROM_PORT="${GMS_PROMETHEUS_PORT:-9090}"
GRAFANA_PORT="${GMS_GRAFANA_PORT:-3001}"

log() { echo "[deploy] $*"; }
die() { echo "[deploy][LOI] $*" >&2; exit 1; }

for cmd in git docker curl; do
  command -v "$cmd" >/dev/null 2>&1 || die "Chua cai $cmd tren may chu (chay scripts/deploy/setup-server.sh)"
done
docker info >/dev/null 2>&1 || die "Tai khoan $(whoami) khong dung duoc Docker. Them vao nhom docker roi dang nhap lai: sudo usermod -aG docker $(whoami)"

# 1. Lay ma nguon
if [ ! -d "$APP_DIR/.git" ]; then
  log "Clone lan dau vao $APP_DIR"
  git clone "$REPO_URL" "$APP_DIR"
fi
cd "$APP_DIR"
git remote set-url origin "$REPO_URL"
git fetch --prune origin "$BRANCH"
TARGET="${COMMIT:-origin/$BRANCH}"
git checkout --force --detach "$TARGET" || die "Khong checkout duoc $TARGET (commit chua co tren nhanh $BRANCH?)"
log "Ma nguon: $(git rev-parse --short HEAD) $(git log -1 --format=%s)"

# Bi mat cua may chu (mat khau DB, JWT, Grafana) nam ngoai repo, o ~/.gms.env; Docker Compose doc qua tep .env
if [ -f "$HOME/.gms.env" ]; then
  cp "$HOME/.gms.env" "$APP_DIR/.env"
  chmod 600 "$APP_DIR/.env"
  log "Dung bi mat tu ~/.gms.env"
else
  log "CANH BAO: chua co ~/.gms.env nen dang dung mat khau/JWT mac dinh cua ban demo (xem docs/CICD_SSH_DEPLOY.md)"
fi

# 2. Cau hinh cho may chu: trinh duyet goi API theo dia chi cong khai cua may chu (nhung vao ma web luc build)
export GMS_PUBLIC_API_URL="http://${PUBLIC_HOST}:${API_PORT}"
export GMS_API_PORT GMS_WEB_PORT GMS_PROMETHEUS_PORT GMS_GRAFANA_PORT
COMPOSE=(docker compose -f docker-compose.demo.yml -f docker-compose.monitoring.yml)

# 3. Dung lai he thong
if [ -z "$("${COMPOSE[@]}" ps -aq sqlserver 2>/dev/null)" ]; then
  log "Lan dau: dung toan bo he thong (SQL Server, Redis, backend, nap du lieu mau, frontend, giam sat). Mat 5 den 10 phut."
  "${COMPOSE[@]}" up -d --build
else
  log "Cap nhat backend, frontend, Prometheus, Grafana (giu nguyen SQL Server, Redis, du lieu)"
  "${COMPOSE[@]}" up -d --build backend frontend prometheus grafana
fi
# Prometheus doc lai cau hinh va luat canh bao khi cac tep nay doi (tep mount, container khong tu tao lai)
"${COMPOSE[@]}" kill -s SIGHUP prometheus >/dev/null 2>&1 || true

# 4. Cho tat ca san sang
wait_for() { # ten url so_lan
  local name="$1" url="$2" tries="${3:-60}" i
  for ((i = 1; i <= tries; i++)); do
    if curl -fsS -o /dev/null --max-time 10 "$url"; then log "OK  $name  $url"; return 0; fi
    sleep 5
  done
  echo "[deploy][LOI] $name khong san sang: $url" >&2
  "${COMPOSE[@]}" ps >&2 || true
  "${COMPOSE[@]}" logs --tail=60 backend >&2 || true
  return 1
}
wait_for "API (backend)" "http://localhost:${API_PORT}/actuator/health" 72
wait_for "Web (frontend)" "http://localhost:${WEB_PORT}/" 36
wait_for "Prometheus" "http://localhost:${PROM_PORT}/-/ready" 24
wait_for "Grafana" "http://localhost:${GRAFANA_PORT}/api/health" 24

# Prometheus phai lay duoc so lieu cua backend (can vai chuc giay sau khi backend len)
for ((i = 1; i <= 12; i++)); do
  if curl -fsSG --max-time 10 --data-urlencode 'query=max(up{job="gms-backend"})' \
       "http://localhost:${PROM_PORT}/api/v1/query" | grep -q ',"1"\]'; then
    log "OK  Prometheus dang thu thap so lieu cua gms-backend"; break
  fi
  if [ "$i" -eq 12 ]; then
    log "CANH BAO: chua thay gms-backend o trang thai up (xem http://${PUBLIC_HOST}:${PROM_PORT}/targets)"
  fi
  sleep 5
done

docker image prune -f >/dev/null 2>&1 || true
log "XONG. Web http://${PUBLIC_HOST}:${WEB_PORT}  API http://${PUBLIC_HOST}:${API_PORT}/swagger-ui.html  Grafana http://${PUBLIC_HOST}:${GRAFANA_PORT}"
