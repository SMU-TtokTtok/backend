-- V2: ClubMember에서 User 외래키 제거 (안전한 방식)

-- 1. 백업 테이블 생성 (안전장치)
CREATE TABLE IF NOT EXISTS club_members_backup_v2 AS
SELECT * FROM club_members;

-- 2. 인덱스 삭제 (존재한다면)
DROP INDEX IF EXISTS idx_club_members_member_id;

-- 3. 외래키 제거 (있다면)
ALTER TABLE club_members DROP CONSTRAINT IF EXISTS fk_club_members_member_id;

-- 4. member_id 컬럼 삭제
ALTER TABLE club_members DROP COLUMN IF EXISTS member_id;
