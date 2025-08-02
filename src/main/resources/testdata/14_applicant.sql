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
 NOW(), NOW()),

('applicant-018', 'user18@example.com', '이승환', 22, '전자공학과', 'applicant18@gmail.com', '010-1802-1234', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-019', 'user19@example.com', '조성민', 20, '컴퓨터공학과', 'applicant19@gmail.com', '010-1903-2345', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-020', 'user20@example.com', '홍승우', 21, '체육학과', 'applicant20@gmail.com', '010-2004-3456', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),

('applicant-021', 'user21@example.com', '오진우', 20, '기계공학과', 'applicant21@gmail.com', '010-2105-4567', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-022', 'user22@example.com', '이현민', 22, '전자공학과', 'applicant22@gmail.com', '010-2206-5678', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-023', 'user23@example.com', '정도현', 21, '컴퓨터공학과', 'applicant23@gmail.com', '010-2307-6789', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-024', 'user24@example.com', '김승현', 19, '체육학과', 'applicant24@gmail.com', '010-2408-7890', 'ENROLLED', 'FIRST_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-025', 'user25@example.com', '노진혁', 20, '기계공학과', 'applicant25@gmail.com', '010-2509-8901', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-026', 'user26@example.com', '하민수', 22, '전자공학과', 'applicant26@gmail.com', '010-2601-9012', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-027', 'user27@example.com', '남기훈', 21, '컴퓨터공학과', 'applicant27@gmail.com', '010-2702-0123', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-028', 'user28@example.com', '오승찬', 19, '체육학과', 'applicant28@gmail.com', '010-2803-1234', 'ENROLLED', 'FIRST_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-029', 'user29@example.com', '김재훈', 22, '기계공학과', 'applicant29@gmail.com', '010-2904-2345', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-030', 'user30@example.com', '신민호', 20, '전자공학과', 'applicant30@gmail.com', '010-3005-3456', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),

('applicant-031', 'user31@example.com', '장진우', 21, '컴퓨터공학과', 'applicant31@gmail.com', '010-3106-4567', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-032', 'user32@example.com', '김우빈', 19, '체육학과', 'applicant32@gmail.com', '010-3207-5678', 'ENROLLED', 'FIRST_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-033', 'user33@example.com', '박동현', 22, '기계공학과', 'applicant33@gmail.com', '010-3308-6789', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-034', 'user34@example.com', '이정훈', 20, '전자공학과', 'applicant34@gmail.com', '010-3409-7890', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-035', 'user35@example.com', '윤준호', 21, '컴퓨터공학과', 'applicant35@gmail.com', '010-3501-8901', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-036', 'user36@example.com', '최지훈', 23, '체육학과', 'applicant36@gmail.com', '010-3602-9012', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-037', 'user37@example.com', '장민재', 20, '기계공학과', 'applicant37@gmail.com', '010-3703-0123', 'ENROLLED', 'SECOND_GRADE', 'MALE', 'INTERVIEW_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-038', 'user38@example.com', '김정훈', 19, '전자공학과', 'applicant38@gmail.com', '010-3804-1234', 'ENROLLED', 'FIRST_GRADE', 'MALE', 'INTERVIEW_PASS', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-039', 'user39@example.com', '박현수', 22, '컴퓨터공학과', 'applicant39@gmail.com', '010-3905-2345', 'ENROLLED', 'FOURTH_GRADE', 'MALE', 'INTERVIEW_FAIL', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW()),
('applicant-040', 'user40@example.com', '오지훈', 21, '체육학과', 'applicant40@gmail.com', '010-4006-3456', 'ENROLLED', 'THIRD_GRADE', 'MALE', 'DOCUMENT_EVALUATING', (SELECT id FROM applyforms WHERE club_id = 'club-002' AND status = 'ACTIVE' LIMIT 1), NOW(), NOW());


