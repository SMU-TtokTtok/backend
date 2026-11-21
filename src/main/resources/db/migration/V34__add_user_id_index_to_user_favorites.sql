-- 로그인 사용자의 즐겨찾기 동아리 id 조회용 인덱스 (#464)
-- (SELECT f.club_id FROM user_favorites f JOIN users u ON f.user_id = u.id WHERE u.email = ?)
-- 인기 목록은 후보를 Redis 에 캐시하고 북마크만 요청마다 이 쿼리로 덧씌운다. 인덱스가 없으면 요청마다 user_favorites 를 전부 읽는다.
-- 측정(동아리 300, 즐겨찾기 약 3만, 150 RPS): 로그인 요청 p95 10.6ms -> 6.8ms. load-test/results/issue-464 참고.
-- PostgreSQL은 FK에 자동 인덱스를 만들지 않으므로 명시적으로 생성한다. (V25와 동일 패턴)
CREATE INDEX idx_user_favorites_user_id ON user_favorites (user_id);
