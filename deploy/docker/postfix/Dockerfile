# 릴레이 전용 Postfix.
# 이 서버 IP(110.35.178.38)는 PTR 없음 + Spamhaus PBL 등재라 직접 발송이
# 불가능하다. 따라서 MX 수신도, 직접 발송도 하지 않고 오직
# "사내 SMTP 수신 → 외부 릴레이로 전달" 만 한다.
FROM alpine:3.20

RUN apk add --no-cache \
      postfix \
      cyrus-sasl cyrus-sasl-plain cyrus-sasl-login \
      ca-certificates tzdata \
 && rm -rf /var/cache/apk/*

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 25
ENTRYPOINT ["/entrypoint.sh"]
