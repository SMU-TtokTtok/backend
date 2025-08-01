-- 14. 지원자 데이터 (기존 ApplyForm에 맞춰서 생성)
-- ApplyForm ID들을 참조하여 지원자 생성

-- 먼저 ApplyForm ID들을 조회해야 하므로, 실제 환경에서는 해당 ID들로 교체 필요
-- 여기서는 예시 UUID를 사용하고, 실제로는 7_apply_form.sql에서 생성된 ID를 사용해야 함

INSERT INTO applicants (
    id, user_email, name, age, major, email, phone,
    student_status, grade, gender, current_phase, applyform_id,
    created_at, updated_at
) VALUES

-- 그림사랑 동아리 지원자들 (club-001)
('applicant-001', 'user1@example.com', '김지영', 20, '미술학과', 'applicant1@gmail.com', '010-1234-5678',
 'ENROLLED', 'SECOND_GRADE', 'FEMALE', 'DOCUMENT_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-001' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-002', 'user2@example.com', '박민수', 22, '디자인학과', 'applicant2@gmail.com', '010-2345-6789',
 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-001' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-003', 'user3@example.com', '이수진', 19, '회화과', 'applicant3@gmail.com', '010-3456-7890',
 'ENROLLED', 'FIRST_GRADE', 'FEMALE', 'DOCUMENT_FAIL',
 (SELECT id FROM applyforms WHERE club_id = 'club-001' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 상명축구부 지원자들 (club-002) - 면접 있음
('applicant-004', 'user4@example.com', '최대현', 21, '체육학과', 'applicant4@gmail.com', '010-4567-8901',
 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-005', 'user5@example.com', '정민호', 20, '기계공학과', 'applicant5@gmail.com', '010-5678-9012',
 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-006', 'user6@example.com', '김태준', 23, '전자공학과', 'applicant6@gmail.com', '010-6789-0123',
 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-007', 'user7@example.com', '송지훈', 19, '컴퓨터공학과', 'applicant7@gmail.com', '010-7890-1234',
 'ENROLLED', 'FIRST_GRADE', 'MALE', 'INTERVIEW_FAIL',
 (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 코딩마스터 지원자들 (club-003)
('applicant-008', 'user8@example.com', '한소영', 21, '컴퓨터공학과', 'applicant8@gmail.com', '010-8901-2345',
 'ENROLLED', 'THIRD_GRADE', 'FEMALE', 'DOCUMENT_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-003' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-009', 'user9@example.com', '오준석', 22, '소프트웨어학과', 'applicant9@gmail.com', '010-9012-3456',
 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-003' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-010', 'user10@example.com', '강지원', 20, '정보보안학과', 'applicant10@gmail.com', '010-0123-4567',
 'ABSENCE', 'SECOND_GRADE', 'FEMALE', 'DOCUMENT_FAIL',
 (SELECT id FROM applyforms WHERE club_id = 'club-003' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 나눔봉사단 지원자들 (club-004)
('applicant-011', 'user11@example.com', '윤서현', 19, '사회복지학과', 'applicant11@gmail.com', '010-1111-2222',
 'ENROLLED', 'FIRST_GRADE', 'FEMALE', 'DOCUMENT_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-004' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-012', 'user12@example.com', '임동욱', 21, '경영학과', 'applicant12@gmail.com', '010-2222-3333',
 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-004' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 농구동아리 지원자들 (club-006) - 면접 있음
('applicant-013', 'user13@example.com', '배성민', 20, '체육학과', 'applicant13@gmail.com', '010-3333-4444',
 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-006' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-014', 'user14@example.com', '홍지수', 22, '간호학과', 'applicant14@gmail.com', '010-4444-5555',
 'ENROLLED', 'THIRD_GRADE', 'FEMALE', 'DOCUMENT_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-006' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 영어회화 지원자들 (club-007)
('applicant-015', 'user15@example.com', '서지혜', 21, '영어영문학과', 'applicant15@gmail.com', '010-5555-6666',
 'ENROLLED', 'THIRD_GRADE', 'FEMALE', 'DOCUMENT_EVALUATING',
 (SELECT id FROM applyforms WHERE club_id = 'club-007' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

-- 댄스크루 지원자들 (club-012) - 면접 있음
('applicant-016', 'user16@example.com', '조예린', 19, '무용학과', 'applicant16@gmail.com', '010-6666-7777',
 'ENROLLED', 'FIRST_GRADE', 'FEMALE', 'INTERVIEW_PASS',
 (SELECT id FROM applyforms WHERE club_id = 'club-012' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW()),

('applicant-017', 'user17@example.com', '김동현', 20, '연극영화학과', 'applicant17@gmail.com', '010-7777-8888',
 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_FAIL',
 (SELECT id FROM applyforms WHERE club_id = 'club-012' AND status = 'ACTIVE' LIMIT 1),
 NOW(), NOW());
