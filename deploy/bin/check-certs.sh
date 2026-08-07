#!/usr/bin/env bash
#
# 인증서 만료 감시. cron 이 매일 돌린다.
#
# 갱신은 ttokttok-certbot 컨테이너가 12시간마다 시도하지만, 실패해도 오류가
# 컨테이너 로그로만 간다. 갱신은 만료 30일 전부터 시작하므로 실패가 누적되면
# 30일 뒤 조용히 만료되고, HTTPS 가 죽고 나서야 알게 된다. 이 스크립트가
# 그 사이에 사람에게 알리는 유일한 경로다.
#
#   check-certs.sh
#
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
ENV_FILE="$ROOT/app/.env"
WARN_DAYS="${WARN_DAYS:-21}"   # 갱신 시작(30일)이 두 번 이상 실패해야 도달하는 값

# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a
: "${DOMAIN:?.env 에 DOMAIN 이 필요하다}"
: "${FILE_DOMAIN:?.env 에 FILE_DOMAIN 이 필요하다}"

log() { printf '[check-certs] %s\n' "$*"; }

# 발급 전이면 조용히 끝낸다. 아직 HTTPS 로 전환하지 않은 단계에서 매일 경고가
# 오면 진짜 경고를 무시하게 된다.
if ! compgen -G "$ROOT/data/certbot/conf/live/*/" >/dev/null; then
    log "발급된 인증서 없음 — 건너뜀"
    exit 0
fi

# 파일이 아니라 **실제로 서빙 중인 인증서**를 본다. 파일만 보면
# "갱신은 됐지만 nginx 가 reload 되지 않아 옛 인증서를 계속 내보내는" 상태를
# 놓친다. 클라이언트가 보는 것이 곧 진실이다.
days_left() {
    local host="$1" end
    end="$(echo | openssl s_client -connect 127.0.0.1:443 -servername "$host" 2>/dev/null \
           | openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)" || return 1
    [[ -n "$end" ]] || return 1
    echo $(( ( $(date -d "$end" +%s) - $(date +%s) ) / 86400 ))
}

problems=()
for h in "$DOMAIN" "$FILE_DOMAIN"; do
    if d="$(days_left "$h")"; then
        log "$h: ${d}일 남음"
        (( d < WARN_DAYS )) && problems+=("$h — 만료까지 ${d}일")
    else
        log "$h: 인증서를 읽지 못했다"
        problems+=("$h — HTTPS 응답 없음 (nginx 중단 또는 설정 오류)")
    fi
done

[[ ${#problems[@]} -eq 0 ]] && exit 0

# ── 경고 메일 ───────────────────────────────────────────────────────────────
TO="${CERT_EMAIL:-}"
if [[ -z "$TO" ]]; then
    log "CERT_EMAIL 이 비어 있어 메일을 보내지 못한다. 문제: ${problems[*]}"
    exit 1
fi

# 컨테이너 안에서의 이름은 RELAY_USER 다 (compose 가 SMTP_RELAY_USER 를 바꿔 넘긴다).
# 네이버는 인증 계정과 다른 From 을 거절하므로 반드시 이 값을 써야 한다.
FROM="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' ttokttok-smtp 2>/dev/null \
        | grep '^RELAY_USER=' | cut -d= -f2- || true)"
if [[ -z "$FROM" ]]; then
    log "smtp 컨테이너에서 RELAY_USER 를 찾지 못했다. 문제: ${problems[*]}"
    exit 1
fi

log "경고 메일 발송 → $TO"
{
    printf 'From: TtokTtok <%s>\n' "$FROM"
    printf 'To: %s\n' "$TO"
    printf 'Subject: [TtokTtok] HTTPS 인증서 경고\n'
    printf 'Content-Type: text/plain; charset=UTF-8\n\n'
    printf '자체 서버의 인증서 상태에 문제가 있다.\n\n'
    printf '  - %s\n' "${problems[@]}"
    printf '\n확인:\n'
    printf '  docker logs --tail 50 ttokttok-certbot\n'
    printf '  cd %s/app && docker compose exec -T nginx nginx -s reload\n' "$ROOT"
    printf '  %s/bin/issue-cert.sh    # 재발급이 필요한 경우\n' "$ROOT"
    printf '\n검사 시각: %s\n' "$(date '+%F %T %Z')"
} | docker exec -i ttokttok-smtp sh -c "sendmail -f '$FROM' '$TO'"

exit 1
