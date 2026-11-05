-- 읽기 전용 사전 점검. 이 파일은 Flyway 마이그레이션으로 등록하지 않는다.
-- 이메일 참조의 user_id 전환과 UNIQUE 추가 전에 운영 상태를 확인한다.
-- 결과에 개인정보가 나오지 않도록 건수만 조회한다. 어떤 데이터도 자동으로 삭제하지 않는다.

-- 1. 사용자 참조 고아. 0이 아니면 해당 행의 처리 방법을 정하기 전에 user_id NOT NULL을 적용할 수 없다.
SELECT 'applicants.user_email' AS reference,
       COUNT(*) AS total,
       COUNT(*) FILTER (WHERE a.user_email IS NULL OR a.user_email = '') AS blank,
       COUNT(*) FILTER (WHERE a.user_email <> '' AND u.id IS NULL) AS orphan,
       COUNT(*) FILTER (WHERE u.id IS NULL AND ci.id IS NOT NULL) AS case_only_match
FROM applicants a
         LEFT JOIN users u ON u.email = a.user_email
         LEFT JOIN users ci ON lower(ci.email) = lower(a.user_email)
UNION ALL
SELECT 'temp_applicants.user_email',
       COUNT(*),
       COUNT(*) FILTER (WHERE t.user_email IS NULL OR t.user_email = ''),
       COUNT(*) FILTER (WHERE t.user_email <> '' AND u.id IS NULL),
       COUNT(*) FILTER (WHERE u.id IS NULL AND ci.id IS NOT NULL)
FROM temp_applicants t
         LEFT JOIN users u ON u.email = t.user_email
         LEFT JOIN users ci ON lower(ci.email) = lower(t.user_email)
UNION ALL
SELECT 'fcm_token.email',
       COUNT(*),
       COUNT(*) FILTER (WHERE f.email IS NULL OR f.email = ''),
       COUNT(*) FILTER (WHERE f.email <> '' AND u.id IS NULL),
       COUNT(*) FILTER (WHERE u.id IS NULL AND ci.id IS NOT NULL)
FROM fcm_token f
         LEFT JOIN users u ON u.email = f.email
         LEFT JOIN users ci ON lower(ci.email) = lower(f.email);

-- 2. 대소문자만 다른 사용자. 0이 아니면 이메일로 user_id를 채울 때 대상이 모호하다.
SELECT COUNT(*) AS ambiguous_user_email_groups
FROM (SELECT lower(email)
      FROM users
      GROUP BY lower(email)
      HAVING COUNT(*) > 1) AS duplicated;

-- 3. FK가 없는 ID 참조의 고아. 0이 아니면 FK 추가 전에 정리 여부를 결정한다.
SELECT 'temp_applicants.form_id' AS reference, COUNT(*) AS orphan
FROM temp_applicants t
         LEFT JOIN applyforms f ON f.id = t.form_id
WHERE f.id IS NULL
UNION ALL
SELECT 'temp_applyforms.club_id', COUNT(*)
FROM temp_applyforms t
         LEFT JOIN clubs c ON c.id = t.club_id
WHERE c.id IS NULL;

-- 4. UNIQUE 추가를 막는 중복. 모두 0이어야 한다. club_members는 check_club_member_duplicates.sql을 함께 실행한다.
SELECT 'user_favorites(user_id, club_id)' AS target, COUNT(*) AS duplicated_groups
FROM (SELECT 1
      FROM user_favorites
      GROUP BY user_id, club_id
      HAVING COUNT(*) > 1) AS duplicated
UNION ALL
SELECT 'applyforms ACTIVE per club', COUNT(*)
FROM (SELECT 1
      FROM applyforms
      WHERE status = 'ACTIVE'
      GROUP BY club_id
      HAVING COUNT(*) > 1) AS duplicated;

-- 5. 적용 이력. 마이그레이션 번호와 운영 기준점을 확인한다.
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- 6. 실제 제약과 인덱스. 코드 기준 판단을 운영 스키마와 대조한다.
SELECT conrelid::regclass::text AS table_name,
       conname,
       contype::text,
       pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE connamespace = current_schema()::regnamespace
  AND contype IN ('f', 'u')
ORDER BY 1, 2;

SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
ORDER BY tablename, indexname;

-- 7. 테이블 규모. 인덱스 생성과 백필의 잠금 시간을 판단하는 근거다. 통계 기반 추정치다.
SELECT relname AS table_name, n_live_tup AS estimated_rows
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;
