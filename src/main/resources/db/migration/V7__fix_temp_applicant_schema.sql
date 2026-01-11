-- TempApplicant 엔티티 구조 변경: id, form_id, created_at, updated_at을 제외한 모든 필드 제거

-- 기존 필드들 삭제
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS name;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS age;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS major;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS email;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS phone;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS student_status;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS grade;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS gender;
ALTER TABLE temp_applicants DROP COLUMN IF EXISTS answers;

-- 사용자가 임시 저장한 데이터를 저장할 JSONB 컬럼 추가
ALTER TABLE temp_applicants ADD COLUMN temp_data JSONB;
