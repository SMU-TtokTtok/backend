#!/usr/bin/env bash
#
# 일일 백업. cron 이 ttokttokuser 로 실행한다 (setup.sh 가 등록).
# 백업본은 /home 파티션(160G)에 쌓인다.
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
OUT="${BACKUP_DIR:-/home/ttokttokuser/backup}"
KEEP_DAYS="${KEEP_DAYS:-14}"

# shellcheck disable=SC1091
set -a; . "$ROOT/app/.env"; set +a

mkdir -p "$OUT"
ts="$(date +%Y%m%d-%H%M%S)"
cd "$ROOT/app"

# ── DB ───────────────────────────────────────────────────────────────────
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
    | gzip > "$OUT/db-$ts.sql.gz"
echo "[backup] db-$ts.sql.gz ($(du -h "$OUT/db-$ts.sql.gz" | cut -f1))"

# ── 업로드 파일 (일요일에만 — 용량이 크므로) ─────────────────────────────
# MinIO 의 온디스크 포맷(.minio.sys, xl.meta)을 그대로 tar 하면 다른 버전/배치로
# 복원할 때 깨질 수 있다. mc mirror 로 평범한 파일 트리를 뽑아 압축한다.
if [[ "$(date +%u)" == "7" ]]; then
    stage="$(mktemp -d)"
    trap 'rm -rf "$stage"' EXIT
    docker run --rm --network ttokttok \
        -e MC_HOST_svc="http://${MINIO_ROOT_USER}:${MINIO_ROOT_PASSWORD}@minio:9000" \
        -v "$stage:/out" \
        minio/mc:RELEASE.2025-08-13T08-35-41Z \
        mirror --quiet --overwrite "svc/${MINIO_BUCKET}" /out
    tar -czf "$OUT/files-$ts.tar.gz" -C "$stage" .
    echo "[backup] files-$ts.tar.gz ($(du -h "$OUT/files-$ts.tar.gz" | cut -f1))"
fi

# ── 오래된 백업 정리 ─────────────────────────────────────────────────────
find "$OUT" -name 'db-*.sql.gz'    -mtime "+$KEEP_DAYS" -delete
find "$OUT" -name 'files-*.tar.gz' -mtime +60           -delete

echo "[backup] 완료 ($(date '+%F %T'))"
