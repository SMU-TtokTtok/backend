#!/usr/bin/env bash
#
# 블루-그린 무중단 배포.
#
#   deploy.sh <image-tag>
#
# 동작:
#   1) 현재 활성 색의 반대 색으로 새 이미지를 띄운다
#   2) 헬스체크 통과를 기다린다 (실패 시 롤백 없이 그냥 중단 — 구 버전이 계속 서비스)
#   3) nginx upstream 을 새 색으로 바꾸고 reload (진행 중 요청은 기존 워커가 끝까지 처리)
#   4) 드레인 후 구 색 컨테이너를 정리한다
#
set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/ttokttok/app}"
ENV_FILE="$APP_DIR/.env"
STATE_FILE="$APP_DIR/state"
UPSTREAM_FILE="${UPSTREAM_FILE:-/opt/ttokttok/config/nginx/upstream.conf}"

HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-120}"   # 초
DRAIN_SECONDS="${DRAIN_SECONDS:-30}"      # 구 컨테이너 드레인 시간
KEEP_IMAGES="${KEEP_IMAGES:-3}"           # 보관할 과거 이미지 개수
INFRA_SERVICES="${INFRA_SERVICES:-postgres redis minio minio-init smtp nginx certbot}"

log()  { printf '\033[36m[deploy]\033[0m %s\n' "$*"; }
warn() { printf '\033[33m[deploy]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[31m[deploy]\033[0m %s\n' "$*" >&2; exit 1; }

TAG="${1:-}"
[[ -n "$TAG" ]] || die "사용법: deploy.sh <image-tag>"
docker image inspect "ttokttok:$TAG" >/dev/null 2>&1 \
  || die "이미지 ttokttok:$TAG 가 없다. 먼저 docker build 를 해야 한다."

cd "$APP_DIR"
[[ -f "$ENV_FILE" ]] || die "$ENV_FILE 없음"

# ── 색 결정 ──────────────────────────────────────────────────────────────
active="$(cat "$STATE_FILE" 2>/dev/null || echo none)"
case "$active" in
  blue)  target=green ;;
  green) target=blue  ;;
  *)     target=blue; active=none ;;
esac
[[ "$target" == blue ]] && port=18081 || port=18082
log "활성=$active → 배포대상=$target (tag=$TAG, health port=$port)"

# ── .env 의 태그 갱신 (in-place, 원자적으로) ─────────────────────────────
key="$(tr '[:lower:]' '[:upper:]' <<<"$target")_TAG"
prev_tag="$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d= -f2- || true)"
tmp="$(mktemp)"
if grep -qE "^${key}=" "$ENV_FILE"; then
  sed "s|^${key}=.*|${key}=${TAG}|" "$ENV_FILE" >"$tmp"
else
  { cat "$ENV_FILE"; echo "${key}=${TAG}"; } >"$tmp"
fi
cat "$tmp" >"$ENV_FILE"   # inode 유지 (바인드 마운트/권한 보존)
rm -f "$tmp"

restore_env() {
  [[ -n "${prev_tag:-}" ]] || return 0
  local t; t="$(mktemp)"
  sed "s|^${key}=.*|${key}=${prev_tag}|" "$ENV_FILE" >"$t"
  cat "$t" >"$ENV_FILE"; rm -f "$t"
}

# ── 의존 서비스 기동 (멱등) ──────────────────────────────────────────────
log "인프라 서비스 확인 중 ($INFRA_SERVICES)"
# shellcheck disable=SC2086
docker compose up -d $INFRA_SERVICES

# ── 새 색 기동 ───────────────────────────────────────────────────────────
log "app-$target 기동"
docker compose --profile "$target" up -d --no-deps "app-$target"

cleanup_target() {
  warn "app-$target 정리 중"
  docker compose --profile "$target" logs --tail 100 "app-$target" || true
  docker compose --profile "$target" stop "app-$target" >/dev/null 2>&1 || true
  docker compose --profile "$target" rm -f "app-$target" >/dev/null 2>&1 || true
  restore_env
}

# ── 헬스체크 폴링 ────────────────────────────────────────────────────────
log "헬스체크 대기 (최대 ${HEALTH_TIMEOUT}s)"
healthy=0
for ((i = 0; i < HEALTH_TIMEOUT; i++)); do
  # 컨테이너가 죽었으면 기다릴 이유가 없다
  if ! docker ps --filter "name=ttokttok-app-$target" --filter status=running -q | grep -q .; then
    sleep 1
    if ! docker ps --filter "name=ttokttok-app-$target" --filter status=running -q | grep -q .; then
      cleanup_target
      die "app-$target 컨테이너가 기동 중 종료됐다"
    fi
  fi
  # 이 앱에는 actuator 의존성이 없다. /health 가 SecurityWhiteList 에 등록된
  # 공개 엔드포인트이고 {"status":"Healthy"} 를 돌려준다.
  if curl -fsS --max-time 3 "http://127.0.0.1:${port}/health" 2>/dev/null | grep -q '"status":"Healthy"'; then
    healthy=1
    log "헬스체크 통과 (${i}s)"
    break
  fi
  sleep 1
done

if [[ "$healthy" -ne 1 ]]; then
  cleanup_target
  die "헬스체크 실패 — 트래픽 전환하지 않음. 기존 버전($active)이 계속 서비스 중."
fi

# ── 트래픽 전환 ──────────────────────────────────────────────────────────
# upstream.conf 는 단일 파일 바인드 마운트다. inode 를 바꾸면 컨테이너가
# 옛 파일을 계속 보게 되므로 반드시 truncate-write 로 갱신한다.
log "nginx 트래픽 전환: $active → $target"
printf 'set $backend "http://app-%s:8080";\n' "$target" >"$UPSTREAM_FILE"

if ! docker compose exec -T nginx nginx -t; then
  printf 'set $backend "http://app-%s:8080";\n' "$active" >"$UPSTREAM_FILE"
  cleanup_target
  die "nginx 설정 검증 실패 — 전환 취소"
fi
docker compose exec -T nginx nginx -s reload
echo "$target" >"$STATE_FILE"
log "전환 완료. 활성=$target"

# ── 구 버전 드레인 후 정리 ───────────────────────────────────────────────
if [[ "$active" != none && "$active" != "$target" ]]; then
  log "구 버전 app-$active 드레인 ${DRAIN_SECONDS}s"
  sleep "$DRAIN_SECONDS"
  docker compose --profile "$active" stop "app-$active" || true
  docker compose --profile "$active" rm -f "app-$active" || true
  log "app-$active 정리 완료"
fi

# ── 오래된 이미지 정리 ───────────────────────────────────────────────────
in_use="$(grep -E '^(BLUE|GREEN)_TAG=' "$ENV_FILE" | cut -d= -f2- | tr '\n' '|' | sed 's/|$//')"
docker images ttokttok --format '{{.Tag}} {{.CreatedAt}}' \
  | sort -k2 -r | tail -n +$((KEEP_IMAGES + 1)) | awk '{print $1}' \
  | grep -vE "^(${in_use:-none})$" \
  | xargs -r -I{} docker rmi "ttokttok:{}" 2>/dev/null || true

log "배포 성공: ttokttok:$TAG ($target)"
