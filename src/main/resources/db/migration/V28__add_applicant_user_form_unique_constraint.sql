-- #429: 같은 사용자의 같은 모집폼 중복 지원을 DB에서 차단한다. (#426 후속)
-- 충돌을 409로 처리하는 코드(PR #427)가 먼저 배포된 릴리스 이후에만 적용한다.
-- 기존 중복이 있으면 이 스크립트는 실패·롤백된다.
-- 적용 전 deploy/migrations/pending/check_applicant_duplicates.sql 로 중복 0건을 확인한다.
ALTER TABLE applicants
    ADD CONSTRAINT uk_applicants_user_email_applyform
    UNIQUE (user_email, applyform_id);
