-- 인기 동아리 목록 측정용 시드 (이슈 #464)
--
-- 배너(/api/clubs/banner/popular)와 더보기(/api/clubs/popular)는 동아리 전체를 집계하므로
-- 행 수가 곧 측정 조건이다. 운영은 동아리 수십 개 규모이고, "많아도 수백 개"의 상한으로 300개를 심는다.
--
--   admins 300, clubs 300, users 10,000
--   user_favorites 약 3만 건 (사용자당 0~6개, 앞 번호 동아리에 몰리게 제곱 분포)
--   club_members 약 8천 명 (동아리당 0~80명, 제곱 분포)
--   applyforms 동아리당 ACTIVE 1개 (모집 중 / 모집 예정 / 마감 을 1/3씩)
--
-- 부하테스트 컴포즈 DB 전용이다. 다른 측정의 잔여 행이 결과를 흐리지 않도록 관련 테이블을 비우고 다시 심는다.
-- 난수는 setseed 로 고정해 재실행해도 같은 데이터가 나온다.
-- 운영 스키마와 맞추려고 V29 의 club_members(club_id, email) UNIQUE 를 없으면 만든다
-- (컴포즈 앱은 ddl-auto=update 로 스키마를 만들어 마이그레이션 제약이 빠져 있다).

TRUNCATE clubs, admins, users CASCADE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_club_members_club_email') THEN
        ALTER TABLE club_members ADD CONSTRAINT uk_club_members_club_email UNIQUE (club_id, email);
    END IF;
END $$;

SELECT setseed(0.464);

INSERT INTO admins (id, username, password, email, created_at, updated_at)
SELECT format('00000000-0000-4464-a000-%s', lpad(n::text, 12, '0')),
       'lt464_admin_' || n,
       '$2a$10$loadtestdummyhashvaluenotusedatall000000000000000000000000',
       'lt464-admin-' || n || '@ttokttok.dev',
       now(), now()
FROM generate_series(1, 300) n;

INSERT INTO clubs (id, name, summary, club_type, club_category, club_univ, custom_category, content,
                   admin_id, view_count, version, created_at, updated_at)
SELECT format('00000000-0000-4464-c000-%s', lpad(n::text, 12, '0')),
       '부하테스트_동아리_' || n,
       '인기 목록 측정용 동아리 ' || n,
       (ARRAY['CENTRAL', 'UNION', 'DEPARTMENT'])[1 + n % 3],
       (ARRAY['VOLUNTEER', 'CULTURE', 'ACADEMIC', 'SPORTS', 'RELIGION', 'ARTS', 'SOCIAL', 'ETC'])[1 + n % 8],
       (ARRAY['GLOBAL_AREA', 'DESIGN', 'ENGINEERING', 'CONVERGENCE_TECHNOLOGY', 'ARTS'])[1 + n % 5],
       '기타',
       '# 부하테스트 동아리',
       format('00000000-0000-4464-a000-%s', lpad(n::text, 12, '0')),
       floor(power(random(), 2) * 3000)::bigint,
       0,
       now() - (random() * interval '365 days'),
       now()
FROM generate_series(1, 300) n;

INSERT INTO users (id, email, is_email_verified, name, password, terms_agreed, provider, created_at, updated_at)
SELECT format('00000000-0000-4464-b000-%s', lpad(n::text, 12, '0')),
       'lt464-user-' || n || '@ttokttok.dev',
       true, '측정사용자' || n, null, true, 'LOCAL', now(), now()
FROM generate_series(1, 10000) n;

INSERT INTO user_favorites (id, user_id, club_id, created_at, updated_at)
SELECT gen_random_uuid()::text, f.user_id, f.club_id, now(), now()
FROM (
    SELECT DISTINCT
           format('00000000-0000-4464-b000-%s', lpad(u::text, 12, '0')) AS user_id,
           format('00000000-0000-4464-c000-%s', lpad((1 + floor(power(random(), 2) * 300))::int::text, 12, '0')) AS club_id
    FROM generate_series(1, 10000) u
    CROSS JOIN LATERAL generate_series(1, (random() * 6)::int + (u * 0)) k
) f;

INSERT INTO club_members (id, club_id, email, gender, grade, major, member_name, phone_number, role,
                          created_at, updated_at)
SELECT gen_random_uuid()::text,
       format('00000000-0000-4464-c000-%s', lpad(c::text, 12, '0')),
       'lt464-member-' || c || '-' || m || '@ttokttok.dev',
       (ARRAY['MALE', 'FEMALE'])[1 + m % 2],
       (ARRAY['FIRST_GRADE', 'SECOND_GRADE', 'THIRD_GRADE', 'FOURTH_GRADE'])[1 + m % 4],
       '컴퓨터과학과', '부원' || m, '010-0000-0000',
       CASE WHEN m = 1 THEN 'PRESIDENT' ELSE 'MEMBER' END,
       now(), now()
FROM generate_series(1, 300) c
CROSS JOIN LATERAL generate_series(1, floor(power(random(), 2) * 80)::int + (c * 0)) m;

-- 모집 중(n%3=0): 시작 지남, 마감 1~30일 뒤 / 모집 예정(n%3=1): 시작 1~20일 뒤 / 마감(n%3=2): 기간 지남
INSERT INTO applyforms (id, club_id, title, sub_title, form_json, has_interview, is_recruiting, max_apply_count,
                        status, apply_start_date, apply_end_date, created_at, updated_at)
SELECT gen_random_uuid()::text,
       format('00000000-0000-4464-c000-%s', lpad(n::text, 12, '0')),
       '측정용 지원서 ' || n, null, '{}'::jsonb, false, n % 3 <> 2, 100, 'ACTIVE',
       CASE n % 3 WHEN 0 THEN current_date - 5 WHEN 1 THEN current_date + 1 + n % 20 ELSE current_date - 40 END,
       CASE n % 3 WHEN 0 THEN current_date + 1 + n % 30 WHEN 1 THEN current_date + 30 + n % 20 ELSE current_date - 10 END,
       now(), now()
FROM generate_series(1, 300) n;

ANALYZE admins, clubs, users, user_favorites, club_members, applyforms;

SELECT (SELECT count(*) FROM clubs) AS clubs,
       (SELECT count(*) FROM users) AS users,
       (SELECT count(*) FROM user_favorites) AS favorites,
       (SELECT count(*) FROM club_members) AS members,
       (SELECT count(*) FROM applyforms) AS applyforms;
