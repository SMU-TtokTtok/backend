-- #432: 같은 (동아리, 이메일) 부원이 어떤 쓰기 경로로도 중복 저장되지 않도록 DB에서 차단한다.
-- 합격 확정·수동 등록의 동아리 행 잠금 코드(#432)가 먼저 배포된 릴리스 이후에만 적용한다.
-- 기존 중복이 있으면 이 스크립트는 실패·롤백된다.
-- 적용 전 deploy/migrations/pending/check_club_member_duplicates.sql 로 중복 0건을 확인한다.
-- 이 폴더는 Flyway 경로 밖이다. 적용 승인 후 src/main/resources/db/migration/V29__...sql 로 옮긴다.
ALTER TABLE club_members
    ADD CONSTRAINT uk_club_members_club_email
    UNIQUE (club_id, email);
