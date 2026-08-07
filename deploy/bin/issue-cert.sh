#!/usr/bin/env bash
#
# Let's Encrypt 인증서 최초 발급 (webroot 방식). API 도메인과 파일 도메인 둘 다 발급한다.
#
# 선행 조건 — 하나라도 안 되어 있으면 실패한다:
#   - 두 도메인의 DNS A 레코드가 이 서버의 공인 IP 를 가리킴
#   - 공유기에서 외부 80 → 이 PC:80 포워딩
#   - nginx 가 HTTP 모드로 떠 있음 (nginx-apply.sh 로 생성)
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
ENV_FILE="$ROOT/app/.env"
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a
: "${DOMAIN:?.env 에 DOMAIN 이 필요하다}"
: "${FILE_DOMAIN:?.env 에 FILE_DOMAIN 이 필요하다}"
SERVER_NAMES="${SERVER_NAMES:-$DOMAIN}"
FILE_SERVER_NAMES="${FILE_SERVER_NAMES:-$FILE_DOMAIN}"
EMAIL="${CERT_EMAIL:-${1:-}}"
[[ -n "$EMAIL" ]] || { echo "사용법: issue-cert.sh <이메일>  (또는 .env 에 CERT_EMAIL)" >&2; exit 1; }

cd "$ROOT/app"

probe="acme-probe-$$"
echo ok > "$ROOT/data/certbot/www/$probe"
trap 'rm -f "$ROOT/data/certbot/www/$probe"' EXIT

for d in $SERVER_NAMES $FILE_SERVER_NAMES; do
    echo "[cert] 사전 점검: $d 의 ACME 경로가 외부에서 열려 있는지 확인"
    if ! curl -fsS --max-time 10 "http://${d}/.well-known/acme-challenge/${probe}" | grep -q ok; then
        echo "[cert] 실패: http://${d}/.well-known/acme-challenge/ 로 외부 접근이 안 된다." >&2
        echo "       DNS A 레코드와 공유기 80 포트 포워딩을 먼저 확인할 것." >&2
        exit 1
    fi
done
echo "[cert] 사전 점검 통과"

# API 도메인과 파일 도메인은 별도 인증서로 발급한다. 한 장에 묶으면 한쪽 DNS 만
# 어긋나도 갱신 전체가 실패하고, https.conf 가 도메인별 경로를 참조하기 때문이다.
# 같은 서비스의 apex/www 는 --cert-name 아래 한 장으로 묶는다.
issue() {
    local cert_name="$1"; shift
    local args=()
    for d in "$@"; do args+=(-d "$d"); done
    docker compose run --rm --entrypoint certbot certbot \
        certonly --webroot -w /var/www/certbot \
        --cert-name "$cert_name" "${args[@]}" \
        --email "$EMAIL" \
        --agree-tos --no-eff-email \
        --non-interactive
}
# shellcheck disable=SC2086
issue "$DOMAIN"      $SERVER_NAMES
# shellcheck disable=SC2086
issue "$FILE_DOMAIN" $FILE_SERVER_NAMES

echo "[cert] 발급 완료. HTTPS 로 전환한다."
"$ROOT/bin/nginx-apply.sh" --https
