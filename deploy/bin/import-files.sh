#!/usr/bin/env bash
#
# 기존 S3 백업(resources.tar)을 MinIO 버킷에 적재한다. 멱등하다.
#
#   import-files.sh /opt/ttokttok/ttokttok-restore/resources.tar
#
# tar 안의 경로가 그대로 오브젝트 키가 된다 (applicant/<이메일>/<uuid>_<파일명> 등).
# 키가 바뀌면 DB 에 저장된 공개 URL 이 전부 깨지므로 경로를 손대지 않는다.
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
TAR="${1:-$ROOT/ttokttok-restore/resources.tar}"
MC_IMAGE="minio/mc:RELEASE.2025-08-13T08-35-41Z"

[[ -f "$TAR" ]] || { echo "tar 파일이 없다: $TAR" >&2; exit 1; }

# shellcheck disable=SC1091
set -a; . "$ROOT/app/.env"; set +a
: "${MINIO_ROOT_USER:?}" "${MINIO_ROOT_PASSWORD:?}" "${MINIO_BUCKET:?}"

stage="$(mktemp -d)"
trap 'rm -rf "$stage"' EXIT

echo "[import] 전개 중: $TAR"
tar -xf "$TAR" -C "$stage"
echo "[import] 파일 $(find "$stage" -type f | wc -l) 개"

echo "[import] MinIO 적재 → $MINIO_BUCKET"
docker run --rm --network ttokttok \
    -e MC_HOST_svc="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
    -v "$stage:/in:ro" \
    "$MC_IMAGE" \
    mirror --overwrite /in "svc/${MINIO_BUCKET}"

echo "[import] 적재 후 오브젝트 수:"
docker run --rm --network ttokttok \
    -e MC_HOST_svc="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
    "$MC_IMAGE" \
    ls --recursive "svc/${MINIO_BUCKET}" | wc -l
