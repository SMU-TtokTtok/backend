#!/bin/sh
set -e

: "${RELAYHOST:?RELAYHOST 필요 (예: [smtp.gmail.com]:587)}"
: "${MY_HOSTNAME:=mail.localdomain}"
: "${MY_NETWORKS:=172.28.0.0/16}"

postconf -e "myhostname = ${MY_HOSTNAME}"
postconf -e "mydestination ="                 # 로컬 수신 안 함 (MX 아님)
postconf -e "relayhost = ${RELAYHOST}"
postconf -e "inet_interfaces = all"
postconf -e "inet_protocols = ipv4"

# ── 오픈 릴레이 방지 ─────────────────────────────────────────────────────
# compose 네트워크와 루프백에서 온 메일만 중계한다. 호스트에 25 포트를
# publish 하지 않으므로 외부에서는 애초에 접속 자체가 불가능하지만,
# 설정 레벨에서도 한 번 더 막는다.
postconf -e "mynetworks = 127.0.0.0/8 ${MY_NETWORKS}"
postconf -e "smtpd_relay_restrictions = permit_mynetworks, reject"
postconf -e "smtpd_recipient_restrictions = permit_mynetworks, reject_unauth_destination"

# ── 업스트림 릴레이로의 TLS + SASL ───────────────────────────────────────
postconf -e "smtp_tls_security_level = encrypt"
postconf -e "smtp_tls_CAfile = /etc/ssl/certs/ca-certificates.crt"
postconf -e "message_size_limit = 26214400"   # 25MB
postconf -e "maillog_file = /dev/stdout"

if [ -n "${RELAY_USER:-}" ]; then
    postconf -e "smtp_sasl_auth_enable = yes"
    # texthash 는 postmap 없이 평문 파일을 그대로 읽는다 (컨테이너에 적합)
    postconf -e "smtp_sasl_password_maps = texthash:/etc/postfix/sasl_passwd"
    postconf -e "smtp_sasl_security_options = noanonymous"
    postconf -e "smtp_sasl_mechanism_filter = plain, login"
    printf '%s %s:%s\n' "${RELAYHOST}" "${RELAY_USER}" "${RELAY_PASSWORD}" \
        > /etc/postfix/sasl_passwd
    chmod 600 /etc/postfix/sasl_passwd
else
    echo "[postfix] RELAY_USER 없음 — 인증 없이 릴레이 시도 (대부분의 제공자에서 실패한다)" >&2
fi

newaliases 2>/dev/null || true

trap 'postfix stop' TERM INT
exec postfix start-fg
