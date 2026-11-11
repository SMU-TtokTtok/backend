-- #460 측정 전용 seed. 측정 DB(ttokttok460)에서만 쓴다. 실행 때마다 관련 테이블을 비우고 다시 채운다.
-- 사용: psql -v ON_ERROR_STOP=1 -v pass_count=1000 -v fail_count=1000 -f seed-result-mail.sql
-- 동아리 1개, 관리자 1명, 활성 지원 폼 1개, 서류 전형 합격 :pass_count명, 불합격 :fail_count명.
-- 지원자 이메일은 r<번호>@example.test (합격 1..pass_count, 불합격 그 뒤). 실제로 존재하지 않는 도메인이다.
TRUNCATE TABLE result_mail_deliveries, document_phases, applicants, applyform_grades, applyforms,
    clubs, admins CASCADE;

INSERT INTO admins (id, username, password, created_at, updated_at)
VALUES ('crash460-admin', 'crash460-admin', 'not-used', now(), now());

INSERT INTO clubs (id, admin_id, name, summary, content, custom_category, club_univ, club_category, club_type,
                   view_count, created_at, updated_at)
VALUES ('crash460-club', 'crash460-admin', 'crash460', 'crash460', 'crash460', 'crash460',
        'ENGINEERING', 'ETC', 'CENTRAL', 0, now(), now());

INSERT INTO applyforms (id, club_id, title, status, is_recruiting, has_interview, max_apply_count,
                        apply_start_date, apply_end_date, form_json, created_at, updated_at)
VALUES ('crash460-form', 'crash460-club', 'crash460', 'ACTIVE', true, false, 100000,
        current_date - 1, current_date + 10, '[]'::jsonb, now(), now());

INSERT INTO applyform_grades (applyform_id, grades)
VALUES ('crash460-form', 'FIRST_GRADE');

INSERT INTO applicants (id, applyform_id, user_email, name, email, student_status, grade, gender, current_phase,
                        created_at, updated_at)
SELECT 'crash460-a' || n, 'crash460-form', 'u' || n || '@example.test', 'applicant' || n,
       'r' || n || '@example.test', 'ENROLLED', 'FIRST_GRADE', 'MALE', 'DOCUMENT', now(), now()
FROM generate_series(1, :pass_count + :fail_count) AS n;

INSERT INTO document_phases (id, applicant_id, status, created_at, updated_at)
SELECT 'crash460-d' || n, 'crash460-a' || n, CASE WHEN n <= :pass_count THEN 'PASS' ELSE 'FAIL' END, now(), now()
FROM generate_series(1, :pass_count + :fail_count) AS n;
