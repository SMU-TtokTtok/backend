-- public 스키마의 소유권/권한을 정리한다.
--
--   소유자  : MIGRATOR_DB_USER  — Flyway 전용. DDL(ALTER/DROP/CREATE) 가능
--   앱 롤   : APP_DB_USER       — 런타임. DML 만. 소유권 없음
--
-- PostgreSQL 에서 ALTER TABLE / DROP COLUMN 같은 DDL 은 GRANT 로 넘길 수 있는
-- 권한이 아니라 "소유자" 에게만 허용된다. 그래서 앱 롤에 아무리 GRANT 를 더해도
-- Flyway 마이그레이션은 통과하지 못한다. 소유권을 별도 롤로 옮기는 이 단계가
-- 유일한 해법이다. (이슈 #403 — V26 이 `must be owner of table admins` 로 실패)
--
-- 앱 롤에 소유권을 주지 않는 이유: 앱이 탈취되더라도 DROP TABLE 이 불가능한
-- 현재의 최소권한 설계를 유지하기 위해서다.
--
-- 최초 초기화(init-db/02-grants.sh)와 이미 초기화된 DB 소급 적용
-- (bin/db-grant-migrator.sh)이 같은 파일을 쓴다. 양쪽 결과가 어긋날 수 없다.
--
-- 필요한 psql 변수: :migrator_user, :app_user
-- 슈퍼유저(POSTGRES_USER)로 실행해야 한다. 여러 번 실행해도 안전하다.

\set ON_ERROR_STOP on

-- ── 1. 마이그레이션 롤 ──────────────────────────────────────────────────
-- 새 테이블을 만들려면 스키마에 CREATE 가 필요하다.
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'migrator_user')
\gexec

GRANT USAGE, CREATE ON SCHEMA public TO :"migrator_user";

-- ── 2. 기존 객체 소유권을 마이그레이션 롤로 모은다 ──────────────────────
-- 덤프를 postgres 로 복원하면 모든 객체가 postgres 소유로 들어온다.
-- flyway_schema_history 도 포함해야 한다 — Flyway 가 이력을 기록해야 하므로.
SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'migrator_user')
  FROM pg_tables WHERE schemaname = 'public'
\gexec

SELECT format('ALTER SEQUENCE public.%I OWNER TO %I', sequencename, :'migrator_user')
  FROM pg_sequences WHERE schemaname = 'public'
\gexec

SELECT format('ALTER VIEW public.%I OWNER TO %I', viewname, :'migrator_user')
  FROM pg_views WHERE schemaname = 'public'
\gexec

-- ── 3. 앱 롤은 DML 만 ───────────────────────────────────────────────────
SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'app_user')
\gexec

GRANT USAGE ON SCHEMA public TO :"app_user";
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO :"app_user";
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO :"app_user";

-- ── 4. 앞으로 마이그레이션이 만들 객체에도 앱 권한이 붙도록 ─────────────
-- 이게 없으면 새 테이블을 추가하는 마이그레이션마다 앱이 "permission denied"
-- 로 죽는다. 소유자가 바뀌었으므로 기존 postgres 기준 default privileges 로는
-- 덮이지 않는다 — migrator 기준으로 다시 선언해야 한다.
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator_user" IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"migrator_user" IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";
