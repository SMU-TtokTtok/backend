-- 부원 테이블에 사용자 이름을 추가

-- 1. club_members 테이블에 member_name 컬럼 추가
ALTER TABLE club_members ADD COLUMN member_name VARCHAR(255);

-- 2. 기존 데이터의 member_name을 users 테이블에서 가져와서 업데이트
UPDATE club_members
SET member_name = (
    SELECT u.name
    FROM users u
    WHERE u.id = club_members.member_id
);

-- 3. member_name 컬럼을 NOT NULL로 변경
ALTER TABLE club_members ALTER COLUMN member_name SET NOT NULL;

