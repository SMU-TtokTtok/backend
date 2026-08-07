#!/usr/bin/env bash
#
# 실제 앱 없이 배포/프록시 로직을 검증한다.
# 더미 앱 이미지(nginx 기반, /health 응답)와 실제 MinIO 로 다음을 확인:
#   1) 최초 배포 → blue 활성
#   2) 두 번째 배포 → green 전환, 전환 중 요청 유실 0
#   3) 헬스체크 실패 이미지 → 배포 중단, 구 버전이 계속 서비스
#   4) 파일 도메인 → MinIO 프록시 (한글·공백 파일명 포함), 목록 조회 차단
#
set -Eeuo pipefail

TESTROOT="${TESTROOT:-/tmp/bluegreen-test}"
SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROXY_PORT=18080
FILE_DOMAIN=files.localhost
MC_IMAGE="minio/mc:RELEASE.2025-08-13T08-35-41Z"

pass() { printf '\033[32m  ✓ %s\033[0m\n' "$*"; }
fail() { printf '\033[31m  ✗ %s\033[0m\n' "$*"; FAILED=1; }
step() { printf '\n\033[1;36m== %s ==\033[0m\n' "$*"; }
FAILED=0

cleanup() {
    step "정리"
    (cd "$TESTROOT/app" && docker compose --profile blue --profile green down -v --remove-orphans 2>/dev/null) || true
    docker rmi ttokttok:t1 ttokttok:t2 ttokttok:broken 2>/dev/null || true
    # MinIO 가 남긴 파일은 컨테이너 uid 소유일 수 있어 호스트에서 못 지운다.
    docker run --rm -v "$(dirname "$TESTROOT"):/w" alpine rm -rf "/w/$(basename "$TESTROOT")" 2>/dev/null || true
    rm -rf "$TESTROOT" 2>/dev/null || true
}
trap cleanup EXIT

# ── 더미 앱 이미지 ───────────────────────────────────────────────────────
step "더미 앱 이미지 빌드"
mkdir -p "$TESTROOT/dummy"
cat > "$TESTROOT/dummy/Dockerfile" <<'EOF'
FROM nginx:1.27-alpine
ARG CONF=t1.conf
RUN apk add --no-cache curl
COPY ${CONF} /etc/nginx/conf.d/default.conf
EOF

# 정상 응답 버전 두 개 + 헬스체크가 절대 통과하지 않는 버전 하나
for v in t1 t2; do
    cat > "$TESTROOT/dummy/$v.conf" <<EOF
server {
    listen 8080;
    location /health {
        default_type application/json;
        return 200 '{"status":"Healthy"}';
    }
    location / {
        default_type text/plain;
        return 200 "$v\n";
    }
}
EOF
done
cat > "$TESTROOT/dummy/broken.conf" <<'EOF'
server {
    listen 8080;
    location /health {
        default_type application/json;
        return 503 '{"status":"Down"}';
    }
    location / { default_type text/plain; return 503 "broken\n"; }
}
EOF

for v in t1 t2 broken; do
    docker build -q --build-arg "CONF=$v.conf" -t "ttokttok:$v" "$TESTROOT/dummy" >/dev/null
done
pass "t1 / t2 / broken 이미지 준비"

# ── 테스트용 /opt/ttokttok 복제 ──────────────────────────────────────────
step "테스트 루트 구성: $TESTROOT"
# data/* 를 미리 만들어 둔다. 없으면 Docker 가 바인드 마운트 대상을
# root 소유로 생성해버려 정리 때 지울 수 없다.
mkdir -p "$TESTROOT"/{app,config/app,config/nginx/conf.d,config/nginx/templates,logs,docker} \
         "$TESTROOT"/data/{postgres,redis,minio,certbot/conf,certbot/www}
cp -r "$SRC/docker/postfix" "$SRC/docker/minio" "$TESTROOT/docker/"
cp "$SRC/config/nginx/conf.d/proxy-common.inc" "$TESTROOT/config/nginx/conf.d/"
cp "$SRC/config/nginx/upstream.conf"           "$TESTROOT/config/nginx/upstream.conf"

render() {
    sed -e 's|__DOMAIN__|localhost|g' \
        -e 's|__SERVER_NAMES__|localhost|g' \
        -e "s|__FILE_DOMAIN__|$FILE_DOMAIN|g" \
        -e "s|__FILE_SERVER_NAMES__|$FILE_DOMAIN|g" \
        -e 's|__MAX_BODY__|50M|g' \
        -e 's|__MINIO_BUCKET__|ttokttok-files|g' "$1"
}
render "$SRC/config/nginx/templates/http-only.conf"   > "$TESTROOT/config/nginx/conf.d/ttokttok.conf"
render "$SRC/config/nginx/templates/minio-public.inc" > "$TESTROOT/config/nginx/conf.d/minio-public.inc"

# 실제 compose 를 경로/포트만 바꿔서 그대로 사용 (설정 자체를 검증하기 위함)
sed -e "s|/opt/ttokttok|$TESTROOT|g" \
    -e 's|"80:80"|"18080:80"|' -e 's|"443:443"|"18443:443"|' \
    -e 's|127.0.0.1:19000:9000|127.0.0.1:19010:9000|' \
    -e 's|127.0.0.1:19001:9001|127.0.0.1:19011:9001|' \
    -e 's|user: "1001:1003"|user: "0:0"|' \
    "$SRC/docker-compose.yml" > "$TESTROOT/app/docker-compose.yml"

cp "$SRC/deploy.sh" "$TESTROOT/app/deploy.sh"; chmod +x "$TESTROOT/app/deploy.sh"
sed -e 's/^DOMAIN=.*/DOMAIN=localhost/' \
    -e "s/^FILE_DOMAIN=.*/FILE_DOMAIN=$FILE_DOMAIN/" \
    -e "s/^FILE_SERVER_NAMES=.*/FILE_SERVER_NAMES=$FILE_DOMAIN/" \
    -e 's/^SERVER_NAMES=.*/SERVER_NAMES=localhost/' \
    -e 's/^COMPOSE_SUBNET=.*/COMPOSE_SUBNET=172.29.0.0\/16/' \
    -e 's/^MINIO_ROOT_USER=.*/MINIO_ROOT_USER=ttokttok-admin/' \
    -e 's/^MINIO_ROOT_PASSWORD=.*/MINIO_ROOT_PASSWORD=test-minio-root-pw/' \
    -e 's/^MINIO_BUCKET=.*/MINIO_BUCKET=ttokttok-files/' \
    -e 's/^MINIO_APP_SECRET_KEY=.*/MINIO_APP_SECRET_KEY=test-minio-app-pw/' \
    "$SRC/.env.example" > "$TESTROOT/app/.env"
echo none > "$TESTROOT/app/state"
pass "구성 완료"

export APP_DIR="$TESTROOT/app"
export UPSTREAM_FILE="$TESTROOT/config/nginx/upstream.conf"
export INFRA_SERVICES="minio minio-init nginx"   # db/redis/smtp 는 이 테스트에 불필요
export DRAIN_SECONDS=3
export HEALTH_TIMEOUT=60

# ── 1. 최초 배포 ─────────────────────────────────────────────────────────
step "1. 최초 배포 (t1 → blue 예상)"
"$TESTROOT/app/deploy.sh" t1
[[ "$(cat "$TESTROOT/app/state")" == blue ]] && pass "state=blue" || fail "state=$(cat "$TESTROOT/app/state")"
body="$(curl -fsS "http://127.0.0.1:$PROXY_PORT/")"
[[ "$body" == t1 ]] && pass "프록시 응답=t1" || fail "프록시 응답=$body"

# ── 2. 무중단 전환 ───────────────────────────────────────────────────────
step "2. t2 배포 — 전환 중 요청 유실 확인"
LOG="$TESTROOT/probe.log"
( while :; do
      curl -s -o /dev/null -w '%{http_code}\n' --max-time 2 "http://127.0.0.1:$PROXY_PORT/" >> "$LOG" || echo "000" >> "$LOG"
  done ) &
PROBE=$!
sleep 2
"$TESTROOT/app/deploy.sh" t2
sleep 2
kill "$PROBE" 2>/dev/null || true; wait "$PROBE" 2>/dev/null || true

total=$(wc -l < "$LOG")
bad=$(grep -cvx 200 "$LOG" || true)
echo "  요청 $total 건, 비200 응답 $bad 건"
[[ "$bad" -eq 0 ]] && pass "요청 유실 0 (무중단 확인)" || { fail "비200 $bad 건"; sort "$LOG" | uniq -c; }
[[ "$(cat "$TESTROOT/app/state")" == green ]] && pass "state=green" || fail "state=$(cat "$TESTROOT/app/state")"
body="$(curl -fsS "http://127.0.0.1:$PROXY_PORT/")"
[[ "$body" == t2 ]] && pass "프록시 응답=t2" || fail "프록시 응답=$body"
docker ps --filter name=ttokttok-app-blue --filter status=running -q | grep -q . \
    && fail "구 컨테이너(blue)가 아직 살아있다" || pass "구 컨테이너(blue) 정리됨"

# ── 3. 실패 시 롤백(전환 안 함) ──────────────────────────────────────────
step "3. 헬스체크 실패 이미지 배포 — 전환되지 않아야 함"
export HEALTH_TIMEOUT=15
if "$TESTROOT/app/deploy.sh" broken; then
    fail "실패해야 할 배포가 성공으로 끝났다"
else
    pass "deploy.sh 가 0 이 아닌 코드로 종료"
fi
[[ "$(cat "$TESTROOT/app/state")" == green ]] && pass "state 유지=green" || fail "state=$(cat "$TESTROOT/app/state")"
body="$(curl -fsS "http://127.0.0.1:$PROXY_PORT/")"
[[ "$body" == t2 ]] && pass "구 버전(t2)이 계속 서비스 중" || fail "프록시 응답=$body"
grep -q 'GREEN_TAG=t2' "$TESTROOT/app/.env" && pass ".env 태그 롤백됨" || { fail ".env 태그"; grep -E '_TAG=' "$TESTROOT/app/.env"; }

# ── 4. 파일 도메인 → MinIO ───────────────────────────────────────────────
# 실제 백업에 들어 있는 형태의 키(한글 + 공백 + 쉼표)로 확인한다.
step "4. 파일 도메인 프록시"
KEY='applicant/202921019@sangmyung.kr/1ee375ca-865a-41e4-b934-73ff5f578159_스크린샷, 2022-06-13 오후 5.02.18.pdf'
docker run --rm --network ttokttok \
    -e MC_HOST_svc="http://ttokttok-admin:test-minio-root-pw@minio:9000" \
    --entrypoint sh "$MC_IMAGE" -c '
      printf "probe-object-content" > /tmp/o
      mc cp --quiet /tmp/o "svc/ttokttok-files/$1" >/dev/null
    ' _ "$KEY" >/dev/null

# nginx 는 Host 헤더로 서버 블록을 고른다. DNS 없이 Host 만 지정해 호출한다.
body="$(curl -fsS --resolve "$FILE_DOMAIN:$PROXY_PORT:127.0.0.1" \
        --get "http://$FILE_DOMAIN:$PROXY_PORT/$(python3 -c '
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1]))' "$KEY")")" || body="<실패>"
[[ "$body" == "probe-object-content" ]] && pass "한글·공백 포함 키 GET 성공" || fail "파일 응답=$body"

code=$(curl -s -o /dev/null -w '%{http_code}' --resolve "$FILE_DOMAIN:$PROXY_PORT:127.0.0.1" \
       "http://$FILE_DOMAIN:$PROXY_PORT/?list-type=2")
[[ "$code" == 403 || "$code" == 404 ]] && pass "버킷 목록 조회 차단 (HTTP $code)" || fail "목록 조회가 열려 있다 (HTTP $code)"

code=$(curl -s -o /dev/null -w '%{http_code}' -X PUT --data x --resolve "$FILE_DOMAIN:$PROXY_PORT:127.0.0.1" \
       "http://$FILE_DOMAIN:$PROXY_PORT/board-images/should-not-write.txt")
[[ "$code" == 403 ]] && pass "쓰기 요청 차단 (HTTP 403)" || fail "PUT 이 막히지 않았다 (HTTP $code)"

step "결과"
[[ "$FAILED" -eq 0 ]] && { printf '\033[1;32m모든 검증 통과\033[0m\n'; exit 0; } \
                      || { printf '\033[1;31m실패 있음\033[0m\n'; exit 1; }
