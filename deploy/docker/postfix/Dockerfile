# 릴레이 전용 Postfix.
# 이 서버 IP(110.35.178.38)는 PTR 없음 + Spamhaus PBL 등재라 직접 발송이
# 불가능하다. 따라서 MX 수신도, 직접 발송도 하지 않고 오직
# "사내 SMTP 수신 → 외부 릴레이로 전달" 만 한다.
FROM alpine:3.20

# PLAIN 메커니즘(libplain.so)은 별도 패키지가 아니라 cyrus-sasl 본체에 들어 있다.
# Alpine 에 cyrus-sasl-plain 이라는 패키지는 존재하지 않아, 넣으면 빌드가
# "unable to select packages" 로 실패한다. LOGIN 만 별도 패키지다.
RUN apk add --no-cache \
      postfix \
      cyrus-sasl cyrus-sasl-login \
      ca-certificates tzdata \
 && rm -rf /var/cache/apk/* \
 && test -f /usr/lib/sasl2/libplain.so   # PLAIN 가용 여부를 빌드 시점에 못박는다

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 25
ENTRYPOINT ["/entrypoint.sh"]
