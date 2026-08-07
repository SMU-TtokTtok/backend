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
CONFIG_DIR="${CONFIG_DIR:-/opt/ttokttok/config/app}"

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

# 앱 설정 파일 사전 검사.
#
# 이 파일들이 없으면 앱은 어차피 부팅에 실패한다 — jwt.secret, server.url,
# super.admin.* 처럼 기본값 없는 @Value 가 여럿이라 환경변수만으로는 뜨지 않는다.
# 검사가 없어도 서비스는 안전하지만(헬스체크 실패 → 전환 없이 중단 → 구 버전 유지),
# 그 사실이 드러나기까지 HEALTH_TIMEOUT 만큼을 버린다. 여기는 컨테이너를 아직
# 하나도 건드리지 않은 지점이라, 지금 중단하는 것이 가장 싸다.
#
# -s 는 "존재 + 비어있지 않음". base64 디코딩이 빈 파일을 만드는 경우까지 잡는다.
# Firebase 키는 CI 에서도 선택 사항이므로 검사하지 않는다.
for f in application.yml application-prod.yml; do
  [[ -s "$CONFIG_DIR/$f" ]] \
    || die "$CONFIG_DIR/$f 가 없거나 비어 있다. CI 의 시크릿 배치 단계를 확인할 것."
done

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

# 대상 색 컨테이너가 running 인지 compose 에게 직접 묻는다.
# compose ps -q 는 자기 프로젝트의 컨테이너 ID 를 정확히 돌려주므로 이름 규약과 무관하다.
target_running() {
  local cid
  cid="$(docker compose --profile "$1" ps -q "app-$1" 2>/dev/null | head -1)"
  [[ -n "$cid" ]] || return 1
  [[ "$(docker inspect -f '{{.State.Running}}' "$cid" 2>/dev/null)" == "true" ]]
}

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
  # 컨테이너가 죽었으면 기다릴 이유가 없다.
  #
  # 컨테이너를 이름으로 찾지 않고 compose 에게 묻는다. `docker ps --filter name=` 은
  # (1) container_name 이나 프로젝트명이 바뀌면 멀쩡히 도는 컨테이너를 못 찾아
  # 배포를 영구히 실패시키고, (2) 부분 일치라서 ttokttok-app-blue-old 같은
  # 잔여 컨테이너에도 매칭돼 죽은 배포를 살아있다고 오판한다.
  if ! target_running "$target"; then
    sleep 1
    if ! target_running "$target"; then
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

# 컨테이너가 실제로 새 내용을 보는지 확인한다.
#
# 바인드 마운트는 inode 를 물고 있다. 호스트에서 이 파일의 inode 가 한 번이라도
# 바뀌면(install, sed -i, 일부 편집기의 저장 방식 — 전부 교체로 동작한다) 컨테이너는
# 옛 inode 를 계속 보고, 위의 쓰기는 컨테이너에 닿지 않는다.
#
# 이 확인이 없으면 전환 절차가 전부 "성공" 한다. 컨테이너 입장에서는 내용이 안 바뀌었으니
# nginx -t 도 reload 도 통과하고, state 갱신까지 끝난다. 그리고 구 색을 정리하는
# 순간 nginx 가 사라진 컨테이너를 가리킨 채 전 요청이 502 가 된다. 배포는 성공으로
# 보고되고 사이트만 죽는다. 실제로 그렇게 서비스가 내려갔다 — 이슈 #376.
#
# 아직 구 색이 살아 있는 지점이라, 여기서 멈추면 서비스는 무사하다.
if ! docker compose exec -T nginx cat /etc/nginx/upstream.conf 2>/dev/null | grep -q "app-$target"; then
  printf 'set $backend "http://app-%s:8080";\n' "$active" >"$UPSTREAM_FILE"
  cleanup_target
  die "nginx 컨테이너가 upstream.conf 갱신을 보지 못한다 (바인드 마운트 inode 불일치).
       호스트 파일이 교체된 적이 있다. 'docker compose up -d --force-recreate nginx' 로
       마운트를 다시 걸어야 한다. 트래픽은 $active 에 그대로 있다."
fi

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
