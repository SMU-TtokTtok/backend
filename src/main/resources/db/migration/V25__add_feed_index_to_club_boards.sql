-- 게시판 피드 커서 페이지네이션 쿼리용 복합 인덱스
-- (WHERE club_id = ? ORDER BY created_at DESC, id DESC LIMIT n)
-- PostgreSQL은 FK에 자동 인덱스를 만들지 않으므로 명시적으로 생성한다. (notices의 V22 인덱스와 동일 패턴)
CREATE INDEX idx_club_boards_club_id_created_at_id ON club_boards (club_id, created_at DESC, id DESC);
