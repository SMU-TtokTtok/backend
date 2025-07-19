-- 6. 동아리 게시판 데이터 (각 동아리별 2-3개씩)
INSERT INTO club_boards (id, title, content, club_id, created_at, updated_at) VALUES

-- club-001 (그림사랑) 게시글
(gen_random_uuid(), '신입생 환영 전시회 개최 안내', '안녕하세요 그림사랑 동아리입니다.<br/><br/>다가오는 3월에 신입생 환영 전시회를 개최합니다.<br/><img src="board-images/art-exhibition.jpg" /><br/>일시: 3월 15일 오후 2시<br/>장소: 학생회관 1층 전시실', 'club-001', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
(gen_random_uuid(), '정기 모임 일정 변경 공지', '정기 모임 일정이 변경되었습니다.<br/><br/>기존: 매주 화요일 오후 6시<br/>변경: 매주 수요일 오후 7시<br/><br/>많은 양해 부탁드립니다.', 'club-001', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),

-- club-002 (상명축구부) 게시글
(gen_random_uuid(), '신입 부원 모집 안내', '⚽ 상명축구부에서 신입 부원을 모집합니다! ⚽<br/><br/>축구를 사랑하는 모든 분들을 환영합니다.<br/><img src="board-images/soccer-field.jpg" />', 'club-002', NOW() - INTERVAL '19 hours', NOW() - INTERVAL '19 hours'),
(gen_random_uuid(), '정기전 결과 보고', '지난주 정기전 결과를 보고드립니다.<br/><br/>🏆 상명대 vs 서울대: 3-1 승리!<br/>🏆 상명대 vs 연세대: 2-2 무승부', 'club-002', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

-- club-003 (코딩마스터) 게시글  
(gen_random_uuid(), '스프링 부트 스터디 시작!', '💻 새 학기 스프링 부트 스터디를 시작합니다!<br/><br/>Java 기초가 있으신 분들을 대상으로 합니다.<br/><img src="board-images/spring-boot.png" />', 'club-003', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
(gen_random_uuid(), '알고리즘 문제 풀이 세션', '이번 주 알고리즘 문제 풀이 세션 안내입니다.<br/><br/>🔥 주제: 그래프 탐색 (DFS, BFS)<br/>📚 추천 문제: 백준 1260, 2606', 'club-003', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),

-- club-004 (나눔봉사단) 게시글
(gen_random_uuid(), '어린이집 봉사활동 후기', '지난 토요일 어린이집 봉사활동 후기입니다. 😊<br/><br/>아이들과 함께한 시간이 정말 소중했습니다.<br/><img src="board-images/volunteer-kids.jpg" />', 'club-004', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

-- club-012 (댄스크루) 게시글
(gen_random_uuid(), '신입생 오디션 안내', '🕺💃 댄스크루 신입생 오디션을 개최합니다!<br/><br/>장르는 자유입니다. 본인만의 매력을 보여주세요!', 'club-012', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
(gen_random_uuid(), '공연 준비 모임', '다음 달 공연을 위한 준비 모임을 진행합니다.<br/>참여 가능하신 분들은 댓글 부탁드려요!', 'club-012', NOW() - INTERVAL '1 week', NOW() - INTERVAL '1 week');