#!/bin/bash
# Postgres 최초 초기화(PGDATA 가 비었을 때)에서 가장 먼저 실행된다.
#
#   00-roles.sh     ← 여기. 롤만 만든다
#   01-restore.sql  ← 덤프 복원 (setup.sh 가 배치한다)
#   02-grants.sh    ← 소유권 이전 + 권한 부여
#
# 롤 생성이 복원보다 먼저여야 하는 이유:
# 이 스택의 백업(bin/backup-db.sh)은 pg_dump 결과를 그대로 쓴다. 소유권을
# 마이그레이션 롤로 옮긴 뒤부터는 덤프에 `ALTER TABLE ... OWNER TO
# ttokttok_migrator` 가 포함되고, 롤이 없는 상태로 복원하면
# `role "ttokttok_migrator" does not exist` 로 실패한다. 즉 재해복구가 막힌다.
# 지금 덤프(소유자 전부 postgres)에는 해당되지 않지만, 다음 백업부터는 그렇다.
set -e

if [ -z "${APP_DB_USER:-}" ] || [ -z "${APP_DB_PASSWORD:-}" ]; then
    echo "[init] APP_DB_USER/APP_DB_PASSWORD 없음 — 롤 생성 생략"
    exit 0
fi

# 마이그레이션 롤이 없으면 Flyway 는 앱 롤로 접속하게 되고, 그 롤은 소유자가
# 아니라서 첫 DDL 마이그레이션에서 배포가 실패한다. 조용히 넘어가면 안 된다.
if [ -z "${MIGRATOR_DB_USER:-}" ] || [ -z "${MIGRATOR_DB_PASSWORD:-}" ]; then
    echo "[init] MIGRATOR_DB_USER/MIGRATOR_DB_PASSWORD 가 .env 에 없다." >&2
    echo "[init] Flyway 가 DDL 을 실행할 수 없게 되므로 초기화를 중단한다. (이슈 #403)" >&2
    exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${APP_DB_USER}') THEN
            CREATE ROLE ${APP_DB_USER} LOGIN PASSWORD '${APP_DB_PASSWORD}';
        END IF;

        IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${MIGRATOR_DB_USER}') THEN
            CREATE ROLE ${MIGRATOR_DB_USER} LOGIN PASSWORD '${MIGRATOR_DB_PASSWORD}';
        END IF;
    END
    \$\$;
EOSQL

echo "[init] 롤 생성 완료 — 앱=${APP_DB_USER}, 마이그레이션=${MIGRATOR_DB_USER}"
