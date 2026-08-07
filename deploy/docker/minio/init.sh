#!/bin/sh
#
# MinIO 초기화. compose 의 minio-init 서비스가 실행하며 멱등하다.
#
#   1) 버킷 생성
#   2) 익명 읽기 정책 — 오브젝트 GET 만 허용하고 목록 조회는 막는다
#   3) 앱 전용 서비스 계정 생성 + 해당 버킷에만 읽기/쓰기 권한 부여
#
set -eu

: "${MINIO_ROOT_USER:?}"
: "${MINIO_ROOT_PASSWORD:?}"
: "${MINIO_BUCKET:?}"
: "${MINIO_APP_ACCESS_KEY:?}"
: "${MINIO_APP_SECRET_KEY:?}"

mc alias set svc "http://minio:9000" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null

mc mb --ignore-existing "svc/$MINIO_BUCKET"

# ── 익명 접근 정책 ────────────────────────────────────────────────────────
# `mc anonymous set download` 는 s3:GetObject 와 함께 s3:ListBucket 까지 열어준다.
# 그러면 https://<파일도메인>/?list-type=2 로 전체 오브젝트 키가 노출되는데,
# 지원자 서류 경로에 이메일이 들어 있어(applicant/<이메일>/…) 그대로 개인정보 유출이다.
# CloudFront 와 동일하게 "키를 아는 경우에만 GET" 이 되도록 정책을 직접 준다.
cat > /tmp/public-read.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"AWS": ["*"]},
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::$MINIO_BUCKET/*"]
    }
  ]
}
EOF
mc anonymous set-json /tmp/public-read.json "svc/$MINIO_BUCKET"

# ── 앱 전용 계정 ──────────────────────────────────────────────────────────
# 앱 설정에 root 자격증명을 두지 않기 위해, 이 버킷에만 권한이 있는 계정을 쓴다.
cat > /tmp/app-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"],
      "Resource": ["arn:aws:s3:::$MINIO_BUCKET/*"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
      "Resource": ["arn:aws:s3:::$MINIO_BUCKET"]
    }
  ]
}
EOF
mc admin policy create svc ttokttok-app /tmp/app-policy.json 2>/dev/null || \
  mc admin policy create svc ttokttok-app /tmp/app-policy.json --force 2>/dev/null || true

# 계정이 이미 있으면 비밀번호를 현재 .env 값으로 맞춘다(멱등).
mc admin user add svc "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY"
mc admin policy attach svc ttokttok-app --user "$MINIO_APP_ACCESS_KEY" 2>/dev/null || true

rm -f /tmp/public-read.json /tmp/app-policy.json

echo "[minio-init] 버킷 '$MINIO_BUCKET' 준비 완료 (익명 GET 허용 / 목록 차단, 앱 계정 연결)"
