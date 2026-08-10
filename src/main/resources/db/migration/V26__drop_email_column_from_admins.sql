-- admins 테이블에서 email 컬럼 제거 (더 이상 관리자 이메일 인증 기능에서 사용하지 않음)
ALTER TABLE admins
    DROP CONSTRAINT IF EXISTS uk_admins_email;

ALTER TABLE admins
    DROP COLUMN IF EXISTS email;
