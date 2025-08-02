-- 16. InterviewPhase 데이터 (면접이 있는 동아리의 서류 합격자들만)
-- 상명축구부(club-002), 농구동아리(club-006), 댄스크루(club-012)는 면접이 있음

INSERT INTO interview_phases (
    id, applicant_id, interview_date, status, created_at, updated_at
) VALUES

-- 상명축구부 지원자들 (면접 있음)
-- applicant-005: INTERVIEW_EVALUATING
('interview-phase-001', 'applicant-005', '2025-03-28', 'EVALUATING', NOW(), NOW()),

-- applicant-006: INTERVIEW_PASS
('interview-phase-002', 'applicant-006', '2025-03-26', 'PASS', NOW(), NOW()),

-- applicant-007: INTERVIEW_FAIL
('interview-phase-003', 'applicant-007', '2025-03-25', 'FAIL', NOW(), NOW()),

-- 농구동아리 지원자들 (면접 있음)
-- applicant-013: INTERVIEW_EVALUATING
('interview-phase-004', 'applicant-013', '2025-03-30', 'EVALUATING', NOW(), NOW()),

-- applicant-014: 서류 합격 상태이므로 면접 일정만 잡힌 상태 (아직 면접 진행 전)
('interview-phase-005', 'applicant-014', '2025-04-02', 'EVALUATING', NOW(), NOW()),

-- 댄스크루 지원자들 (면접 있음)
-- applicant-016: INTERVIEW_PASS
('interview-phase-006', 'applicant-016', '2025-03-27', 'PASS', NOW(), NOW()),

-- applicant-017: INTERVIEW_FAIL
('interview-phase-007', 'applicant-017', '2025-03-29', 'FAIL', NOW(), NOW()),

('interview-phase-018', 'applicant-018', '2025-03-30', 'PASS', NOW(), NOW()),
('interview-phase-019', 'applicant-019', '2025-03-30', 'FAIL', NOW(), NOW()),
('interview-phase-021', 'applicant-021', '2025-04-02', 'PASS', NOW(), NOW()),
('interview-phase-023', 'applicant-023', '2025-04-03', 'FAIL', NOW(), NOW()),
('interview-phase-025', 'applicant-025', '2025-04-03', 'FAIL', NOW(), NOW()),
('interview-phase-026', 'applicant-026', '2025-04-07', 'EVALUATING', NOW(), NOW()),
('interview-phase-029', 'applicant-029', '2025-04-10', 'EVALUATING', NOW(), NOW()),
('interview-phase-030', 'applicant-030', '2025-04-11', 'PASS', NOW(), NOW()),
('interview-phase-033', 'applicant-033', '2025-03-31', 'PASS', NOW(), NOW()),
('interview-phase-034', 'applicant-034', '2025-04-01', 'FAIL', NOW(), NOW()),
('interview-phase-035', 'applicant-035', '2025-04-01', 'PASS', NOW(), NOW()),
('interview-phase-037', 'applicant-037', '2025-04-01', 'PASS', NOW(), NOW()),
('interview-phase-038', 'applicant-038', '2025-04-01', 'PASS', NOW(), NOW()),
('interview-phase-039', 'applicant-039', '2025-04-05', 'EVALUATING', NOW(), NOW());