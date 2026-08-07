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

# 프로브는 certbot 이 실제로 쓰는 경로에 둬야 한다. nginx 는
#   location /.well-known/acme-challenge/ { root /var/www/certbot; }
# 이므로 /var/www/certbot/.well-known/acme-challenge/<파일> 을 찾는다. root 는 alias 와
# 달리 요청 경로를 잘라내지 않는다. 웹루트 최상단에 두면 전부 정상이어도 404 다.
probe="acme-probe-$$"
probe_dir="$ROOT/data/certbot/www/.well-known/acme-challenge"
mkdir -p "$probe_dir"
echo ok > "$probe_dir/$probe"
trap 'rm -f "$probe_dir/$probe"' EXIT

# 이름 해석은 공개 리졸버에 직접 묻는다. 로컬 리졸버(systemd-resolved → ISP)가
# 죽어 있어도 Let's Encrypt 는 자기 리졸버로 우리를 찾으므로, 로컬 해석 실패는
# 발급 가능 여부와 아무 상관이 없다. 실제로 네임서버 변경 직후 ISP 리졸버가
# 오래된 SERVFAIL 을 붙들고 있어 발급이 막히는 일이 있었다. 여기서 확인하려는 것은
# "공개 DNS 가 가리키는 곳이 우리 nginx 인가" 이지 "이 PC 가 이름을 풀 수 있는가"
# 가 아니다. DoH 를 쓰는 이유는 dig 의존성을 늘리지 않기 위해서다 (curl 은 이미 쓴다).
resolve_public() {
    local host="$1" r ip
    for r in 1.1.1.1 8.8.8.8 9.9.9.9; do
        ip="$(curl -fsS --max-time 6 -H 'accept: application/dns-json' \
                "https://${r}/dns-query?name=${host}&type=A" 2>/dev/null \
              | grep -oE '"data":"[0-9]{1,3}(\.[0-9]{1,3}){3}"' | tail -1 | cut -d'"' -f4)" || true
        [[ -n "$ip" ]] && { echo "$ip"; return 0; }
    done
    return 1
}

for d in $SERVER_NAMES $FILE_SERVER_NAMES; do
    echo "[cert] 사전 점검: $d 의 ACME 경로가 외부에서 열려 있는지 확인"
    if ! ip="$(resolve_public "$d")"; then
        echo "[cert] 실패: 공개 DNS 가 $d 의 A 레코드를 돌려주지 않는다." >&2
        echo "       네임서버 위임과 A/CNAME 레코드를 먼저 확인할 것." >&2
        exit 1
    fi
    echo "[cert]   공개 DNS 해석: $d → $ip"
    if ! curl -fsS --max-time 10 --resolve "${d}:80:${ip}" \
              "http://${d}/.well-known/acme-challenge/${probe}" | grep -q ok; then
        echo "[cert] 실패: http://${d}/.well-known/acme-challenge/ 로 접근이 안 된다 (→ $ip)." >&2
        echo "       공유기 80 포트 포워딩과 nginx 상태를 확인할 것." >&2
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

# 갱신 데몬을 여기서 띄운다. compose 에 정의는 있지만 기동 목록에서 빠지기 쉽고,
# 배포가 한 번도 없으면 deploy.sh 의 INFRA_SERVICES 도 실행되지 않는다.
# 발급 직후가 갱신 경로를 세우기에 가장 자연스러운 지점이다.
echo "[cert] 갱신 데몬 기동 (12시간 주기)"
docker compose up -d certbot

echo
echo "갱신 체계:"
echo "  - ttokttok-certbot   12시간마다 certbot renew (만료 30일 전부터 실제 갱신)"
echo "  - cron 04:30         nginx reload (갱신된 인증서 반영)"
echo "  - cron 05:00         check-certs.sh — 만료 임박 시 경고 메일"
