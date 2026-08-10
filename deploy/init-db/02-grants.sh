#!/bin/bash
# 덤프 복원(01-restore.sql) 다음에 실행된다. 복원으로 들어온 객체의 소유권을
# 마이그레이션 롤로 옮기고, 앱 롤에는 DML 만 준다.
#
# 실제 SQL 은 lib/ownership.sql 한 곳에 있다 — 이미 초기화된 운영 DB 에
# 소급 적용하는 bin/db-grant-migrator.sh 와 같은 파일을 써서 결과가 어긋나지
# 않게 한다. (init 스크립트는 PGDATA 가 비었을 때만 돌기 때문에, 운영 DB 에는
# 이 파일이 영원히 실행되지 않는다.)
set -e

if [ -z "${APP_DB_USER:-}" ] || [ -z "${MIGRATOR_DB_USER:-}" ]; then
    echo "[init] APP_DB_USER/MIGRATOR_DB_USER 없음 — 권한 부여 생략"
    exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v migrator_user="$MIGRATOR_DB_USER" \
     -v app_user="$APP_DB_USER" \
     -f /docker-entrypoint-initdb.d/lib/ownership.sql

echo "[init] 소유권 → ${MIGRATOR_DB_USER}, 앱 권한(DML) → ${APP_DB_USER}"
