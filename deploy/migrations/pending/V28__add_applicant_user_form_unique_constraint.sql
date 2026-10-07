-- #426: 409 충돌 처리 코드가 먼저 배포된 후속 릴리스에서만 적용한다.
-- 기존 중복/NULL 및 실제 제약을 사전 점검하고 운영 적용 시간을 결정한다.
-- 적용 직전에 Flyway 최신 버전을 확인하여 필요하면 V28을 변경한다.
-- 승인된 후속 릴리스에서 src/main/resources/db/migration으로 이동한다.
ALTER TABLE applicants
    ADD CONSTRAINT uk_applicants_user_email_applyform
    UNIQUE (user_email, applyform_id);
