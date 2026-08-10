#!/bin/bash
# Postgres 최초 초기화 시 제일 먼저 실행되도록 파일명을 01-restore.sql 보다
# 알파벳 순으로 앞에 둔다. 롤이 먼저 있어야 한다 — 복원되는 덤프의 객체는
# (앞으로) 이 롤들 소유로 찍혀 있어서, 롤이 없으면 "ALTER ... OWNER TO ..." 가
# "role does not exist" 로 실패한다. POSTGRES_USER 를 postgres 로 고정하는 것과
# 같은 이유다 (README 참고).
#
# 여기서는 롤 생성만 한다. GRANT/소유권 이관은 복원 이후 02-grants.sh 가 한다 —
# "GRANT ... ON ALL TABLES", "REASSIGN OWNED BY" 류는 그 시점에 테이블이 실제로
# 있어야 의미가 있다.
set -e

create_role() {
    local user="$1" password="$2"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${user}') THEN
                CREATE ROLE ${user} LOGIN PASSWORD '${password}';
            END IF;
        END
        \$\$;
EOSQL
}

if [ -n "${APP_DB_USER:-}" ] && [ -n "${APP_DB_PASSWORD:-}" ]; then
    create_role "$APP_DB_USER" "$APP_DB_PASSWORD"
    echo "[init] 앱 런타임 롤 ${APP_DB_USER} 생성"
else
    echo "[init] APP_DB_USER/APP_DB_PASSWORD 없음 — 앱 런타임 롤 생성 생략"
fi

# 마이그레이션(Flyway) 전용 롤. 앱 런타임 롤은 최소 권한(DML)만 가지므로
# ALTER/DROP 같은 DDL 을 실행할 수 없다 — 이 롤이 그 몫을 담당한다.
if [ -n "${MIGRATOR_DB_USER:-}" ] && [ -n "${MIGRATOR_DB_PASSWORD:-}" ]; then
    create_role "$MIGRATOR_DB_USER" "$MIGRATOR_DB_PASSWORD"
    echo "[init] 마이그레이션 롤 ${MIGRATOR_DB_USER} 생성"
else
    echo "[init] MIGRATOR_DB_USER/MIGRATOR_DB_PASSWORD 없음 — 마이그레이션 롤 생성 생략"
fi
