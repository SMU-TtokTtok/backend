-- 기존 NULL 값이 있는지 확인하고 기본값으로 업데이트
UPDATE temp_applicants
SET user_email = ''
WHERE user_email IS NULL;

-- NOT NULL 제약 조건 추가
ALTER TABLE temp_applicants
    ALTER COLUMN user_email SET NOT NULL;
