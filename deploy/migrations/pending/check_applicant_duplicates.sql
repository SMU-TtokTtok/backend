-- 읽기 전용 사전 점검. 이 파일은 Flyway 마이그레이션으로 등록하지 않는다.

-- 결과가 없어야 한다. 중복을 자동으로 삭제하지 않는다.
SELECT user_email, applyform_id, COUNT(*) AS application_count
FROM applicants
GROUP BY user_email, applyform_id
HAVING COUNT(*) > 1;

-- 0이어야 한다. UNIQUE만으로 NULL 조합의 중복까지 차단하지는 않는다.
SELECT COUNT(*) AS null_key_count
FROM applicants
WHERE user_email IS NULL OR applyform_id IS NULL;

-- 같은 목적의 제약/인덱스가 이미 있는지 실제 운영 스키마와 대조한다.
SELECT conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'applicants'::regclass AND contype = 'u';

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema() AND tablename = 'applicants';
