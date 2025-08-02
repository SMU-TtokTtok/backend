-- 1학년만 모집 (신입생 전용)
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, 'FIRST_GRADE'
FROM applyforms af
WHERE af.club_id IN ('club-001', 'club-007');

-- 1,2학년 모집 (저학년 대상)
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, grade_value
FROM applyforms af
CROSS JOIN (VALUES ('FIRST_GRADE'), ('SECOND_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-002', 'club-008');

-- 3,4학년 모집 (고학년 대상)  
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, grade_value
FROM applyforms af
CROSS JOIN (VALUES ('THIRD_GRADE'), ('FOURTH_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-003', 'club-010');

-- 전체 학년 모집 (모든 학년 환영)
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, grade_value
FROM applyforms af
CROSS JOIN (VALUES ('FIRST_GRADE'), ('SECOND_GRADE'), ('THIRD_GRADE'), ('FOURTH_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-004', 'club-006', 'club-011', 'club-012');

-- ENGINEERING 과동아리 학년별 모집 데이터 추가 (과동아리 필터링 테스트용)
-- 1,2학년 모집 (저학년 대상)
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, grade_value
FROM applyforms af
CROSS JOIN (VALUES ('FIRST_GRADE'), ('SECOND_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-032', 'club-033');

-- 3,4학년 모집 (고학년 대상)
INSERT INTO applyform_grades (applyform_id, grades) 
SELECT af.id, grade_value
FROM applyforms af
CROSS JOIN (VALUES ('THIRD_GRADE'), ('FOURTH_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-034');

INSERT INTO applyform_grades (applyform_id, grades)
SELECT af.id, grade_value
FROM applyforms af
         CROSS JOIN (VALUES ('FIRST_GRADE'), ('SECOND_GRADE')) AS grades(grade_value)
WHERE af.club_id IN ('club-035');