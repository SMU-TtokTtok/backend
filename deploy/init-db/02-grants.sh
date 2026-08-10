#!/bin/bash
# 00-roles.sh 로 롤이 만들어지고, 01-restore.sql 로 덤프가 복원된 다음 실행된다
# (파일명이 알파벳 순으로 뒤라서). 여기서부터는 테이블이 이미 존재한다는 전제로
# 권한을 부여한다.
#
# 두 롤의 몫을 분리한다:
#   - APP_DB_USER: 앱 런타임. DML(SELECT/INSERT/UPDATE/DELETE)만 가능하다.
#     계정이 뚫려도 스키마 변경/DROP 은 못 하게 하는 최소 권한 원칙.
#   - MIGRATOR_DB_USER: 마이그레이션(Flyway) 전용. 덤프 객체(현재 postgres 소유)의
#     소유권을 이어받아 ALTER/DROP 같은 DDL 을 실행한다.
#
# #403: 이관(#354) 이후 Flyway 가 APP_DB_USER 로 접속해 V26 의 DROP COLUMN 을
# 실행하려다 "must be owner of table admins" 로 실패했다. 이 소유권 분리가 그 수정이다.
set -e

if [ -n "${APP_DB_USER:-}" ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${APP_DB_USER};
        GRANT USAGE ON SCHEMA public TO ${APP_DB_USER};

        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${APP_DB_USER};
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${APP_DB_USER};
EOSQL
    echo "[init] ${APP_DB_USER} DML 권한 부여 완료"
else
    echo "[init] APP_DB_USER 없음 — 앱 런타임 권한 부여 생략"
fi

if [ -n "${MIGRATOR_DB_USER:-}" ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${MIGRATOR_DB_USER};
        GRANT CREATE, USAGE ON SCHEMA public TO ${MIGRATOR_DB_USER};

        -- 덤프 복원 직후 모든 객체(테이블/시퀀스/뷰 등)는 postgres 소유다.
        -- Flyway 의 ALTER/DROP 이 통과하려면 소유자여야 하므로 여기서 넘긴다.
        REASSIGN OWNED BY ${POSTGRES_USER} TO ${MIGRATOR_DB_USER};
EOSQL

    if [ -n "${APP_DB_USER:-}" ]; then
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
            -- 앞으로 마이그레이션(=migrator 가 생성)이 새 테이블/시퀀스를 만들 때도
            -- 앱 런타임 롤이 자동으로 DML 권한을 받게 한다.
            ALTER DEFAULT PRIVILEGES FOR ROLE ${MIGRATOR_DB_USER} IN SCHEMA public
                GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${APP_DB_USER};
            ALTER DEFAULT PRIVILEGES FOR ROLE ${MIGRATOR_DB_USER} IN SCHEMA public
                GRANT USAGE, SELECT ON SEQUENCES TO ${APP_DB_USER};
EOSQL
    fi
    echo "[init] ${MIGRATOR_DB_USER} 소유권 이관 및 기본 권한 설정 완료"
else
    echo "[init] MIGRATOR_DB_USER 없음 — 소유권 이관 생략 (Flyway 가 DDL 을 실행하지 못한다)"
fi
