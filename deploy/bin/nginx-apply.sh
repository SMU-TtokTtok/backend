#!/usr/bin/env bash
#
# nginx 서버 블록을 생성/교체한다.
#
#   nginx-apply.sh            # HTTP 전용 (인증서 발급 전)
#   nginx-apply.sh --https    # HTTPS (인증서 발급 후)
#
# DOMAIN/SERVER_NAMES, FILE_DOMAIN/FILE_SERVER_NAMES, MAX_BODY, MINIO_BUCKET 을
# /opt/ttokttok/app/.env 에서 읽는다.
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
ENV_FILE="$ROOT/app/.env"
TPL_DIR="$ROOT/config/nginx/templates"
CONF_DIR="$ROOT/config/nginx/conf.d"
OUT="$CONF_DIR/ttokttok.conf"

# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a
: "${DOMAIN:?.env 에 DOMAIN 이 필요하다}"
: "${FILE_DOMAIN:?.env 에 FILE_DOMAIN 이 필요하다}"
: "${MINIO_BUCKET:?.env 에 MINIO_BUCKET 이 필요하다}"
MAX_BODY="${MAX_BODY:-50M}"
# server_name 목록을 안 주면 대표 도메인 하나만 받는다.
SERVER_NAMES="${SERVER_NAMES:-$DOMAIN}"
FILE_SERVER_NAMES="${FILE_SERVER_NAMES:-$FILE_DOMAIN}"

render() {
    sed -e "s|__DOMAIN__|$DOMAIN|g" \
        -e "s|__SERVER_NAMES__|$SERVER_NAMES|g" \
        -e "s|__FILE_DOMAIN__|$FILE_DOMAIN|g" \
        -e "s|__FILE_SERVER_NAMES__|$FILE_SERVER_NAMES|g" \
        -e "s|__MAX_BODY__|$MAX_BODY|g" \
        -e "s|__MINIO_BUCKET__|$MINIO_BUCKET|g" \
        "$1"
}

if [[ "${1:-}" == "--https" ]]; then
    src="$TPL_DIR/https.conf"
    # 존재 확인은 컨테이너 안에서 한다. certbot 은 live/ 와 archive/ 를 0700 root 로
    # 만들기 때문에, 호스트에서 ttokttokuser 로 stat 하면 발급이 정상으로 끝났어도
    # "없음" 으로 보인다. 실제로 이 인증서를 읽는 주체는 nginx 컨테이너(root)이고,
    # 거기서 읽히는지가 유일하게 의미 있는 판정이다.
    for d in "$DOMAIN" "$FILE_DOMAIN"; do
        docker run --rm --entrypoint test \
            -v "$ROOT/data/certbot/conf:/etc/letsencrypt:ro" \
            certbot/certbot:latest -f "/etc/letsencrypt/live/$d/fullchain.pem" \
          || { echo "인증서가 없다: live/$d/fullchain.pem — 먼저 issue-cert.sh 를 실행해야 한다" >&2; exit 1; }
    done
else
    src="$TPL_DIR/http-only.conf"
fi

render "$src"                        > "$OUT"
render "$TPL_DIR/minio-public.inc"   > "$CONF_DIR/minio-public.inc"
echo "[nginx] $(basename "$src") → $OUT (api=$SERVER_NAMES / files=$FILE_SERVER_NAMES / bucket=$MINIO_BUCKET / max_body=$MAX_BODY)"

if docker ps --filter name=ttokttok-nginx --filter status=running -q | grep -q .; then
    cd "$ROOT/app"
    docker compose exec -T nginx nginx -t
    docker compose exec -T nginx nginx -s reload
    echo "[nginx] reload 완료"
else
    echo "[nginx] 컨테이너 미기동 — 다음 기동 시 반영된다"
fi
