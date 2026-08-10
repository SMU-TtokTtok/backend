-- 수축(contract) 단계. V26 에서 NOT NULL 을 풀어둔 admins.email 을 실제로 제거한다.
--
-- 확장 단계와 나눠 배포하는 이유는 AGENTS.md 의 "Database Migrations" 를 참고.
-- 이 시점에는 선행 조건이 이미 충족돼 있어 블루-그린 오버랩 구간이 안전하다:
--
--   1. V26 이 운영에 적용됐다 (installed_by = ttokttok_migrator, email 은 nullable).
--   2. email 을 참조하던 코드는 그 배포에서 이미 빠졌다 — Admin 엔티티에 매핑이 없다.
--
-- 즉 이 마이그레이션이 도는 동안 같은 DB 를 쓰는 구 버전 컨테이너도 email 을
-- 읽지 않는다. V26 을 건너뛰고 한 번에 지웠다면 그 구간에서 관리자 API 가 전부
-- 500 이 났을 것이다.
--
-- 컬럼을 지우면 uk_admins_email 도 함께 사라지지만, 의도를 남기려고 먼저 명시한다.
-- IF EXISTS 를 붙여 이미 정리된 환경에서도 다시 돌 수 있게 한다.
ALTER TABLE admins
    DROP CONSTRAINT IF EXISTS uk_admins_email;

ALTER TABLE admins
    DROP COLUMN IF EXISTS email;
