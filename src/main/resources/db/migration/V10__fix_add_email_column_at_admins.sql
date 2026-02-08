-- admin 테이블에 email 컬럼 추가
-- 1. 먼저 NULL 허용으로 컬럼 추가
ALTER TABLE admins
    ADD COLUMN email VARCHAR(255);

-- 2. 기존 데이터에 고유한 임시 이메일 설정
UPDATE admins
SET email = 'admin_' || id || '@temp.com'
WHERE email IS NULL;

-- 3. NOT NULL 제약조건 추가
ALTER TABLE admins
    ALTER COLUMN email SET NOT NULL;

-- 4. UNIQUE 제약조건 추가
ALTER TABLE admins
    ADD CONSTRAINT uk_admins_email UNIQUE (email);
