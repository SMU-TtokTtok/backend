#!/usr/bin/env bash
#
# 이미 초기화된 DB 에 마이그레이션 롤을 소급 적용한다.
#
#   db-grant-migrator.sh
#
# init-db/ 스크립트는 PGDATA 가 비었을 때에만 실행된다. 운영 DB 는 이미
# 초기화가 끝났으므로 00-roles.sh / 02-grants.sh 가 영원히 돌지 않는다.
# 이 스크립트가 그 두 단계를 실행 중인 컨테이너에 직접 적용한다.
#
# 여러 번 실행해도 안전하다. 실제 SQL 은 init-db/lib/ownership.sql 한 곳에만
# 있고 여기서는 그 파일을 그대로 흘려보낸다 — 최초 초기화 경로와 결과가
# 어긋날 수 없다.
set -Eeuo pipefail

ROOT="${ROOT:-/opt/ttokttok}"
ENV_FILE="$ROOT/app/.env"
SQL_FILE="$ROOT/init-db/lib/ownership.sql"
CONTAINER="${CONTAINER:-ttokttok-postgres}"

# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a
: "${POSTGRES_USER:?.env 에 POSTGRES_USER 가 필요하다}"
: "${POSTGRES_DB:?.env 에 POSTGRES_DB 가 필요하다}"
: "${APP_DB_USER:?.env 에 APP_DB_USER 가 필요하다}"
: "${MIGRATOR_DB_USER:?.env 에 MIGRATOR_DB_USER 가 필요하다}"
: "${MIGRATOR_DB_PASSWORD:?.env 에 MIGRATOR_DB_PASSWORD 가 필요하다}"

[[ -f "$SQL_FILE" ]] || { echo "$SQL_FILE 없음 — setup.sh 를 먼저 실행해야 한다" >&2; exit 1; }
docker ps --filter "name=$CONTAINER" --filter status=running -q | grep -q . \
    || { echo "$CONTAINER 가 실행 중이 아니다" >&2; exit 1; }

psql_run() { docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" "$@"; }

# ── 1. 마이그레이션 롤 ───────────────────────────────────────────────────
# 비밀번호는 매번 덮어쓴다. .env 를 고쳤을 때 DB 쪽이 따라오지 않아
# 배포가 인증 실패로 죽는 상황을 막는다.
echo "[db] 롤 $MIGRATOR_DB_USER 확인"
psql_run -q <<EOSQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${MIGRATOR_DB_USER}') THEN
        CREATE ROLE ${MIGRATOR_DB_USER} LOGIN PASSWORD '${MIGRATOR_DB_PASSWORD}';
    ELSE
        ALTER ROLE ${MIGRATOR_DB_USER} WITH LOGIN PASSWORD '${MIGRATOR_DB_PASSWORD}';
    END IF;
END
\$\$;
EOSQL

# ── 2. 소유권 이전 + 권한 부여 ───────────────────────────────────────────
echo "[db] 소유권/권한 적용 ($SQL_FILE)"
psql_run -v migrator_user="$MIGRATOR_DB_USER" -v app_user="$APP_DB_USER" -f - < "$SQL_FILE"

# ── 3. 결과 확인 ─────────────────────────────────────────────────────────
# 소유자가 하나로 모였는지, 앱 롤이 여전히 DDL 을 못 하는지 둘 다 본다.
# 후자를 확인하지 않으면 최소권한이 조용히 무너져도 알 수 없다.
echo "[db] 소유자 분포:"
psql_run -Atc "select tableowner || ' : ' || count(*) from pg_tables where schemaname='public' group by tableowner;"

if docker exec -i "$CONTAINER" psql -q -U "$APP_DB_USER" -d "$POSTGRES_DB" \
       -c 'CREATE TABLE _ttokttok_privcheck (id int);' >/dev/null 2>&1; then
    docker exec -i "$CONTAINER" psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
        -c 'DROP TABLE IF EXISTS _ttokttok_privcheck;' >/dev/null 2>&1 || true
    echo "[db] 경고: 앱 롤 $APP_DB_USER 이 테이블을 만들 수 있다 — 최소권한이 깨졌다" >&2
    exit 1
fi

echo "[db] 완료. Flyway 는 $MIGRATOR_DB_USER, 런타임은 $APP_DB_USER 로 접속한다."
