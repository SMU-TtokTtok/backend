-- 조회수 동시성 측정용 최소 시드 (이슈 #346)
--
-- GET /api/clubs/{clubId}/content 가 실행하는 쿼리는 네 개다.
--   (1) clubs 단건 조회
--   (2) club_members COUNT 서브쿼리        → 행이 없으면 0
--   (3) user_favorites EXISTS 서브쿼리     → 비로그인 요청에서는 아예 실행되지 않음
--                                            (ClubCustomRepositoryImpl.isFavorite 가 email == null 을 먼저 처리)
--   (4) 해당 club 의 ACTIVE applyform 조회 → 없으면 recruiting = false
--
-- 즉 admin 1행 + club 1행만 있으면 200 응답이 나온다. 측정 대상은 clubs 행 하나에 대한
-- view_count 경합뿐이므로, gitignored 24MB mock 시드(src/main/resources/db/seed/)에 의존하지 않고
-- 하네스를 자기완결적으로 유지한다.
--
-- 부하테스트 전용 UUID(...346 / ...347)를 쓴다. mock 시드의 동아리 행을 덮어쓰지 않기 위해서다 —
-- 측정이 공유 데이터를 변형하면 다른 작업과 간섭하고, view_count 리셋이 남의 데이터를 건드리게 된다.
--
-- 멱등하게 재실행할 수 있도록 삭제 후 삽입한다 (clubs.admin_id FK 때문에 clubs 를 먼저 지운다).

DELETE FROM clubs WHERE id = '00000000-0000-4000-8000-000000000346';
DELETE FROM admins WHERE id = '00000000-0000-4000-8000-000000000347';

INSERT INTO admins (id, username, password, email, created_at, updated_at)
VALUES ('00000000-0000-4000-8000-000000000347',
        'loadtest_admin',
        -- 로그인에 쓰이지 않는 더미 해시. 이 시나리오는 비로그인 GET 만 호출한다.
        '$2a$10$loadtestdummyhashvaluenotusedatall000000000000000000000000',
        'loadtest-admin@ttokttok.dev',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO clubs (id, name, summary, club_type, club_category, club_univ,
                   custom_category, content, admin_id, view_count, created_at, updated_at)
VALUES ('00000000-0000-4000-8000-000000000346',
        '부하테스트_동아리',
        '조회수 동시성 측정 대상 동아리',
        'CENTRAL', 'ACADEMIC', 'GLOBAL_AREA',
        '학술/문화',
        '# 부하테스트 대상' || chr(10) || chr(10) || '조회수 동시성 측정에만 사용되는 행입니다.',
        '00000000-0000-4000-8000-000000000347',
        0,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
