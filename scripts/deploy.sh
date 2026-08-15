#!/usr/bin/env bash
#
# 블루-그린 무중단 배포 스크립트 (EC2 에서 실행).
#
# 흐름:
#   1) 새 이미지 pull, 상시 인프라(postgres/redis/caddy) 보장
#   2) 현재 활성 색(active) 판별 → 반대 색(target) 을 새 이미지로 기동
#   3) target 이 /health 통과(healthy)할 때까지 대기 — 실패 시 target 만 내리고 롤백
#   4) Caddyfile 업스트림을 target 으로 렌더 후 `caddy reload` (무중단 전환)
#   5) 구 버전(active) 을 graceful 종료(stop_grace_period 70s) 후 제거
#
# compose profiles(blue/green) 로 색을 선택 기동하므로 모든 compose 호출에 두 프로파일을 켠다.
set -euo pipefail

cd /opt/d14

COMPOSE="docker compose --profile blue --profile green"
HEALTH_MAX_TRIES=40      # 3s * 40 = 최대 120초 대기
HEALTH_INTERVAL=3

log() { echo "[deploy $(date '+%H:%M:%S')] $*"; }

# 성공/실패와 무관하게 미사용 이미지를 정리한다.
# 실행 중 컨테이너가 참조하는 이미지는 삭제되지 않으므로 현재/신규 배포본은 안전하다.
cleanup() { docker image prune -af || true; }
trap cleanup EXIT

render_caddyfile() {
  # $1: 색(blue|green). 템플릿의 placeholder 를 해당 색 업스트림으로 치환해 Caddyfile 생성.
  sed "s/__APP_UPSTREAM__/app-$1:8080/" Caddyfile.template > Caddyfile
}

running() {
  # $1: 컨테이너 이름. 실행 중이면 0.
  docker ps --format '{{.Names}}' | grep -qx "$1"
}

log "== 배포 전 디스크 사용량 =="
df -h /

# EC2 IAM role 의 ECR ReadOnly 권한으로 docker login
/usr/local/bin/ecr-login.sh

# pull 로 디스크가 소진되지 않도록, 새 이미지를 받기 전에 미사용 이미지를 먼저 정리한다.
docker image prune -af

# 새 이미지 및 인프라 이미지 pull
$COMPOSE pull

# 상시 인프라 기동 (Caddyfile 은 최초 1회만 target 기준으로 렌더 후 caddy 기동)
$COMPOSE up -d postgres redis

# ── 활성 색 판별 → target 결정 ──────────────────────────────
active=""
if running d14-app-blue; then
  active=blue
elif running d14-app-green; then
  active=green
fi
if [ "$active" = blue ]; then target=green; else target=blue; fi
log "active=${active:-none} -> target=$target"

# caddy 가 아직 없으면(최초 배포) target 으로 Caddyfile 렌더 후 기동
if ! running d14-caddy; then
  render_caddyfile "$target"
  $COMPOSE up -d caddy
fi

# ── target(신 버전) 기동 ────────────────────────────────────
log "app-$target 기동 (새 이미지)"
$COMPOSE up -d "app-$target"

# ── target healthcheck 통과 대기 ───────────────────────────
log "app-$target /health 통과 대기 (최대 $((HEALTH_MAX_TRIES * HEALTH_INTERVAL))초)"
healthy=0
for _ in $(seq 1 "$HEALTH_MAX_TRIES"); do
  status=$(docker inspect -f '{{.State.Health.Status}}' "d14-app-$target" 2>/dev/null || echo starting)
  if [ "$status" = healthy ]; then healthy=1; break; fi
  sleep "$HEALTH_INTERVAL"
done

if [ "$healthy" != 1 ]; then
  log "app-$target 헬스체크 실패 — 롤백(신 버전 종료), 활성 색은 ${active:-none} 유지"
  $COMPOSE logs "app-$target" 2>/dev/null | tail -50 || true
  $COMPOSE stop -t 75 "app-$target" || true
  $COMPOSE rm -f "app-$target" || true
  exit 1
fi
log "app-$target healthy"

# ── Caddy 업스트림 전환 (무중단 reload) ─────────────────────
# Caddy 는 배포마다 재생성하지 않고 계속 살려두므로(TLS 인증서 유지·무중단), 오래 살아있는 사이
# 새로 만들어지는 app 컨테이너와 다른 Docker 네트워크에 놓여 app-<color> DNS 해석에 실패할 수 있다.
# (그러면 reload 는 성공해도 실제로는 업스트림에 못 닿아 502.) 전환 직전에 target 과 같은 네트워크로
# Caddy 를 강제 정렬한다. 이미 붙어 있으면 무시된다.
target_net=$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' "d14-app-$target")
if [ -n "$target_net" ]; then
  docker network connect "$target_net" d14-caddy 2>/dev/null \
    && log "caddy 를 네트워크 $target_net 에 연결" \
    || true
fi

render_caddyfile "$target"
docker exec d14-caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
log "caddy reload 완료 -> app-$target 로 트래픽 전환"

# ── 구 버전 graceful 종료 ──────────────────────────────────
if [ -n "$active" ]; then
  log "구 버전 app-$active drain (graceful, 최대 70s) 후 제거"
  $COMPOSE stop -t 75 "app-$active"
  $COMPOSE rm -f "app-$active"
fi

# 최초 전환 시 남아있을 레거시 단일 컨테이너(d14-app) 정리
if docker ps -a --format '{{.Names}}' | grep -qx d14-app; then
  log "레거시 d14-app 컨테이너 제거"
  docker rm -f d14-app || true
fi

log "== 배포 후 디스크 사용량 =="
df -h /
log "배포 완료: 활성 색 = app-$target"
