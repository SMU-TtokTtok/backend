-- 읽기 전용 사전 점검. 이 파일은 Flyway 마이그레이션으로 등록하지 않는다.

-- 결과가 없어야 한다. 중복을 자동으로 삭제하지 않는다.
SELECT club_id, email, COUNT(*) AS member_count
FROM club_members
GROUP BY club_id, email
HAVING COUNT(*) > 1;

-- 0이어야 한다. UNIQUE만으로 NULL 조합의 중복까지 차단하지는 않는다.
SELECT COUNT(*) AS null_key_count
FROM club_members
WHERE club_id IS NULL OR email IS NULL;

-- 참고용. 대소문자만 다른 이메일은 UNIQUE가 중복으로 보지 않는다. 결과가 있으면 정리 여부를 별도로 검토한다.
SELECT club_id, lower(email) AS normalized_email, COUNT(*) AS member_count
FROM club_members
GROUP BY club_id, lower(email)
HAVING COUNT(*) > 1 AND COUNT(DISTINCT email) > 1;

-- 같은 목적의 제약/인덱스가 이미 있는지 실제 운영 스키마와 대조한다.
SELECT conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid = 'club_members'::regclass AND contype = 'u';

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema() AND tablename = 'club_members';
