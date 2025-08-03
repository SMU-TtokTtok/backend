-- 4. ClubMember 데이터 생성 (각 동아리별로 다양한 멤버 수)
-- UUID 생성을 위한 함수 사용

-- club-001 (그림사랑): 15명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-001', 'user-001', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'artlover01@example.com', '010-1234-5678', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'drawing02@example.com', '010-2345-6789', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'sketch03@example.com', '010-3456-7890', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-004', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'paint04@example.com', '010-4567-8901', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-005', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'color05@example.com', '010-5678-9012', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-006', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'canvas06@example.com', '010-6789-0123', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-007', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'brush07@example.com', '010-7890-1234', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-008', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'palette08@example.com', '010-8901-2345', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-009', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'easel09@example.com', '010-9012-3456', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-010', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'pencil10@example.com', '010-0123-4567', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-011', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'crayon11@example.com', '010-1357-2468', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-012', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'marker12@example.com', '010-2468-1357', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pastel13@example.com', '010-3579-2468', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-014', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'watercolor14@example.com', '010-4680-1357', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-015', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'oil15@example.com', '010-5791-2468', 'FEMALE', NOW(), NOW());

-- club-002 (상명축구부): 25명 (가장 인기)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-002', 'user-001', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'soccercap@example.com', '010-1111-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'striker02@example.com', '010-2222-3333', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'midfielder03@example.com', '010-3333-4444', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-004', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'defender04@example.com', '010-4444-5555', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-005', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'goalkeeper05@example.com', '010-5555-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-006', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'winger06@example.com', '010-6666-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-007', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'fullback07@example.com', '010-7777-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-008', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'sweeper08@example.com', '010-8888-9999', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-009', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'libero09@example.com', '010-9999-0000', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-010', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'playmaker10@example.com', '010-0000-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-011', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'target11@example.com', '010-1122-3344', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-012', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'anchor12@example.com', '010-2233-4455', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-013', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'box2box13@example.com', '010-3344-5566', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-014', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'regista14@example.com', '010-4455-6677', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-015', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'trequartista15@example.com', '010-5566-7788', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-016', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'mezzala16@example.com', '010-6677-8899', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-017', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'wingback17@example.com', '010-7788-9900', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-018', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'centreback18@example.com', '010-8899-0011', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-019', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'stopper19@example.com', '010-9900-1122', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-020', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'falsenie20@example.com', '010-0011-2233', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-021', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'inside21@example.com', '010-1133-2244', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-022', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'shadow22@example.com', '010-2244-3355', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-023', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'poacher23@example.com', '010-3355-4466', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-024', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'complete24@example.com', '010-4466-5577', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-025', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'utility25@example.com', '010-5577-6688', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-100', 'FOURTH_GRADE', '소프트노예전공', 'MEMBER', 'tnals72441@daum.net', '010-5577-6688', 'MALE', NOW(), NOW());

-- club-003 (코딩마스터): 18명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-003', 'user-026', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'codemaster@example.com', '010-1010-2020', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-027', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'java27@example.com', '010-2020-3030', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'python28@example.com', '010-3030-4040', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-029', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'javascript29@example.com', '010-4040-5050', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-030', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'react30@example.com', '010-5050-6060', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-031', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'spring31@example.com', '010-6060-7070', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-032', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'nodejs32@example.com', '010-7070-8080', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-033', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'database33@example.com', '010-8080-9090', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-034', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'algorithm34@example.com', '010-9090-0101', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-035', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'datastructure35@example.com', '010-0101-1212', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-036', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'network36@example.com', '010-1212-2323', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-037', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'security37@example.com', '010-2323-3434', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-038', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'ai38@example.com', '010-3434-4545', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-039', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'machinelearning39@example.com', '010-4545-5656', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-040', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'deeplearning40@example.com', '010-5656-6767', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'frontend41@example.com', '010-6767-7878', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-042', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'backend42@example.com', '010-7878-8989', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-043', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'fullstack43@example.com', '010-8989-9090', 'FEMALE', NOW(), NOW());

-- club-004 (나눔봉사단): 12명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-004', 'user-044', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'volunteer44@example.com', '010-4040-1010', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'sharing45@example.com', '010-5050-2020', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-046', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'caring46@example.com', '010-6060-3030', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-047', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'helping47@example.com', '010-7070-4040', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-048', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'kindness48@example.com', '010-8080-5050', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-049', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'compassion49@example.com', '010-9090-6060', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-050', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'warmheart50@example.com', '010-0001-7070', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-001', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'gooddeed1@example.com', '010-1001-8080', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'service2@example.com', '010-2001-9090', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-003', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'donation3@example.com', '010-3001-0001', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-004', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'charity4@example.com', '010-4001-1001', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-005', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'support5@example.com', '010-5001-2001', 'MALE', NOW(), NOW());


-- club-005 (연극반): 8명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-005', 'user-001', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'drama1@example.com', '010-0505-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'theater2@example.com', '010-1505-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-003', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'stage3@example.com', '010-2505-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-004', 'SECOND_GRADE', 'Undeclared', 'MEMBER', 'acting4@example.com', '010-3505-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-005', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'script5@example.com', '010-4505-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-006', 'THIRD_GRADE', 'Undeclared', 'MEMBER', 'rehearsal6@example.com', '010-5505-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-007', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'costume7@example.com', '010-6505-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-008', 'FOURTH_GRADE', 'Undeclared', 'MEMBER', 'lighting8@example.com', '010-7505-8888', 'MALE', NOW(), NOW());

-- club-006 (농구동아리): 14명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-006', 'user-009', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'basketball9@example.com', '010-0606-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-010', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'dunk10@example.com', '010-1606-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-011', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'shoot11@example.com', '010-2606-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-012', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'dribble12@example.com', '010-3606-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pass13@example.com', '010-4606-5555', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-014', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'defense14@example.com', '010-5606-6666', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-015', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'rebound15@example.com', '010-6606-7777', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-016', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'assist16@example.com', '010-7606-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-017', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'steal17@example.com', '010-8606-9999', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-018', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'block18@example.com', '010-9606-0000', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-019', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'fastbreak19@example.com', '010-0006-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-020', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'layup20@example.com', '010-1006-2222', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-021', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'freethrow21@example.com', '010-2006-3333', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-022', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'threept22@example.com', '010-3006-4444', 'MALE', NOW(), NOW());

-- club-007 (영어회화): 9명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-007', 'user-023', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'english23@example.com', '010-0707-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-024', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'speaking24@example.com', '010-1707-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-025', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'listening25@example.com', '010-2707-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-026', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'grammar26@example.com', '010-3707-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-027', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'vocabulary27@example.com', '010-4707-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pronunciation28@example.com', '010-5707-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-029', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'conversation29@example.com', '010-6707-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-030', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'accent30@example.com', '010-7707-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-031', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'fluency31@example.com', '010-8707-9999', 'FEMALE', NOW(), NOW());

-- club-008 (포토클럽): 11명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-008', 'user-032', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'photo32@example.com', '010-0808-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-033', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'camera33@example.com', '010-1808-2222', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-034', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'lens34@example.com', '010-2808-3333', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-035', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'aperture35@example.com', '010-3808-4444', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-036', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'shutter36@example.com', '010-4808-5555', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-037', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'iso37@example.com', '010-5808-6666', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-038', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'exposure38@example.com', '010-6808-7777', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-039', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'focus39@example.com', '010-7808-8888', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-040', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'composition40@example.com', '010-8808-9999', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'lighting41@example.com', '010-9808-0000', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-042', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'portrait42@example.com', '010-0008-1111', 'MALE', NOW(), NOW());

-- club-009 (밴드동아리): 6명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-009', 'user-043', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'band43@example.com', '010-0909-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-044', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'guitar44@example.com', '010-1909-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'bass45@example.com', '010-2909-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-046', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'drums46@example.com', '010-3909-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-047', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'keyboard47@example.com', '010-4909-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-048', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'vocal48@example.com', '010-5909-6666', 'MALE', NOW(), NOW());

-- club-010 (테니스부): 13명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-010', 'user-049', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'tennis49@example.com', '010-1010-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-050', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'racket50@example.com', '010-2010-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-001', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'serve1@example.com', '010-3010-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'volley2@example.com', '010-4010-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'backhand3@example.com', '010-5010-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-004', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'forehand4@example.com', '010-6010-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-005', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'smash5@example.com', '010-7010-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-006', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'lob6@example.com', '010-8010-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-007', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'drop7@example.com', '010-9010-9999', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-008', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'slice8@example.com', '010-0010-0001', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-009', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'topspin9@example.com', '010-1010-0002', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-010', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'baseline10@example.com', '010-2010-0003', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-011', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'netplay11@example.com', '010-3010-0004', 'FEMALE', NOW(), NOW());

-- club-011 (독서모임): 7명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-011', 'user-012', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'book12@example.com', '010-1111-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'novel13@example.com', '010-2111-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-014', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'essay14@example.com', '010-3111-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-015', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'poetry15@example.com', '010-4111-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-016', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'biography16@example.com', '010-5111-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-017', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'history17@example.com', '010-6111-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-018', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'philosophy18@example.com', '010-7111-7777', 'FEMALE', NOW(), NOW());

-- club-012 (댄스크루): 16명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-012', 'user-019', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'dance19@example.com', '010-1212-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-020', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'hiphop20@example.com', '010-2212-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-021', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'jazz21@example.com', '010-3212-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-022', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'ballet22@example.com', '010-4212-4444', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-023', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'contemporary23@example.com', '010-5212-5555', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-024', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'popping24@example.com', '010-6212-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-025', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'locking25@example.com', '010-7212-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-026', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'breaking26@example.com', '010-8212-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-027', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'krump27@example.com', '010-9212-9999', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'waacking28@example.com', '010-0212-0001', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-029', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'vogue29@example.com', '010-1212-0002', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-030', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'house30@example.com', '010-2212-0003', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-031', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'freestyle31@example.com', '010-3212-0004', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-032', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'choreography32@example.com', '010-4212-0005', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-033', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'rhythm33@example.com', '010-5212-0006', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-034', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'groove34@example.com', '010-6212-0007', 'MALE', NOW(), NOW());

-- club-013 (토론동아리): 5명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-013', 'user-035', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'debate35@example.com', '010-1313-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-036', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'argument36@example.com', '010-2313-2222', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-037', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'logic37@example.com', '010-3313-3333', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-038', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'rhetoric38@example.com', '010-4313-4444', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-039', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'persuasion39@example.com', '010-5313-5555', 'MALE', NOW(), NOW());

-- club-014 (환경지키미): 10명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-014', 'user-040', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'green40@example.com', '010-1414-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'eco41@example.com', '010-2414-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-042', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'nature42@example.com', '010-3414-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-043', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'earth43@example.com', '010-4414-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-044', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'planet44@example.com', '010-5414-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'recycle45@example.com', '010-6414-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-046', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'sustainable46@example.com', '010-7414-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-047', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'conserve47@example.com', '010-8414-8888', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-048', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'protect48@example.com', '010-9414-9999', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-049', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'save49@example.com', '010-0000-1111', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-050', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'heal50@example.com', '010-1000-2222', 'FEMALE', NOW(), NOW());

-- club-015 (영화감상): 8명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-015', 'user-001', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'moviebuff01@example.com', '010-1112-1314', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'filmlover02@example.com', '010-2122-2324', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'cinemafan03@example.com', '010-3132-3334', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-004', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'director04@example.com', '010-4142-4344', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-005', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'producer05@example.com', '010-5152-5355', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-006', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'screenwriter06@example.com', '010-6162-6366', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-007', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'actor07@example.com', '010-7172-7377', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-008', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'actress08@example.com', '010-8182-8388', 'MALE', NOW(), NOW());

-- club-016 (기타동아리): 12명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-016', 'user-008', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'guitarist01@example.com', '010-1213-1415', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-009', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'strummer02@example.com', '010-2223-2425', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-010', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pick03@example.com', '010-3234-2526', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-011', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chord04@example.com', '010-4245-2627', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-012', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'scale05@example.com', '010-5256-2728', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'melody06@example.com', '010-6267-2829', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-014', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'harmony07@example.com', '010-7278-2930', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-015', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'rhythm08@example.com', '010-8289-3031', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-016', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'strum09@example.com', '010-9290-3132', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-017', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pickin10@example.com', '010-0301-3233', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-018', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'jam11@example.com', '010-1314-3435', 'FEMALE', NOW(), NOW());

-- club-017 (배드민턴): 4명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-017', 'user-020', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'badminton01@example.com', '010-1113-1517', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-021', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'shuttlecock02@example.com', '010-2113-2527', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-022', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'court03@example.com', '010-3113-3537', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-023', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'racket04@example.com', '010-4113-4547', 'MALE', NOW(), NOW());

-- club-018 (요리연구회): 9명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-018', 'user-024', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'chef01@example.com', '010-1114-1618', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-025', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'cooking02@example.com', '010-2114-2628', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-026', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'recipe03@example.com', '010-3114-3638', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-027', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'ingredient04@example.com', '010-4114-4648', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'kitchen05@example.com', '010-5114-5658', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-029', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'cookware06@example.com', '010-6114-6768', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-030', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'utensils07@example.com', '010-7114-7878', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-031', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'apron08@example.com', '010-8114-8989', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-032', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chefknife09@example.com', '010-9114-9090', 'FEMALE', NOW(), NOW());

-- club-019 (사랑나눔): 11명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-019', 'user-033', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'lovesharing01@example.com', '010-1115-1617', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-034', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'kindness02@example.com', '010-2115-2628', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-035', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'compassion03@example.com', '010-3115-3638', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-036', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'generosity04@example.com', '010-4115-4648', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-037', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'charity05@example.com', '010-5115-5658', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-038', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'philanthropy06@example.com', '010-6115-6768', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-039', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'altruism07@example.com', '010-7115-7878', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-040', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'benevolence08@example.com', '010-8115-8989', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'goodwill09@example.com', '010-9115-9090', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-042', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'warmth10@example.com', '010-0000-1011', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-043', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'tenderness11@example.com', '010-1000-2022', 'FEMALE', NOW(), NOW());

-- club-020 (체스동아리): 3명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-020', 'user-044', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'chessmaster01@example.com', '010-1116-1718', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-020', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'checkmate02@example.com', '010-2116-2728', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-020', 'user-046', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pawn03@example.com', '010-3116-3738', 'FEMALE', NOW(), NOW());

-- club-021 (창작문예): 7명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-021', 'user-047', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'writer01@example.com', '010-1117-1819', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-048', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'poet02@example.com', '010-2117-2920', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-049', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'novelist03@example.com', '010-3117-3931', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-050', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'essayist04@example.com', '010-4117-4941', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-001', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'scriptwriter05@example.com', '010-5117-5952', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'playwright06@example.com', '010-6117-6962', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'lyricist07@example.com', '010-7117-7972', 'FEMALE', NOW(), NOW());

-- club-022 (러닝크루): 15명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-022', 'user-004', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'learner01@example.com', '010-1118-1910', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-005', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'student02@example.com', '010-2118-2920', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-006', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'knowledge03@example.com', '010-3118-3931', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-007', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'wisdom04@example.com', '010-4118-4941', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-008', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'insight05@example.com', '010-5118-5952', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-009', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'intellect06@example.com', '010-6118-6962', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-010', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'scholar07@example.com', '010-7118-7972', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-011', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'academy08@example.com', '010-8118-8982', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-012', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mentor09@example.com', '010-9118-9092', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'tutor10@example.com', '010-0000-1010', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-014', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'coach11@example.com', '010-1000-2020', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-015', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'guide12@example.com', '010-2000-3030', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-016', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'helper13@example.com', '010-3000-4040', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-017', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'supporter14@example.com', '010-4000-5050', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-018', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'partner15@example.com', '010-5000-6060', 'FEMALE', NOW(), NOW());

-- club-023 (과학탐구): 6명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-023', 'user-019', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'scientist01@example.com', '010-1119-2021', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-020', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'researcher02@example.com', '010-2119-3032', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-021', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'biologist03@example.com', '010-3119-4043', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-022', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemist04@example.com', '010-4119-5054', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-023', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'physicist05@example.com', '010-5119-6065', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-024', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'astronomer06@example.com', '010-6119-7076', 'MALE', NOW(), NOW());

-- club-024 (여행동아리): 2명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-024', 'user-025', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'traveler01@example.com', '010-1110-2021', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-024', 'user-026', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'explorer02@example.com', '010-2110-3032', 'MALE', NOW(), NOW());

-- club-025 (카페동아리): 8명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-025', 'user-027', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'barista01@example.com', '010-1111-1111', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'cafe02@example.com', '010-2222-2222', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-029', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'espresso03@example.com', '010-3333-3333', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-030', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'latte04@example.com', '010-4444-4444', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-031', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mocha05@example.com', '010-5555-5555', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-032', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'americano06@example.com', '010-6666-6666', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-033', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'cappuccino07@example.com', '010-7777-7777', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-034', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'frappuccino08@example.com', '010-8888-8888', 'MALE', NOW(), NOW());

-- club-026 (스터디모임): 13명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-026', 'user-035', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'study01@example.com', '010-1112-1314', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-036', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'homework02@example.com', '010-2122-2324', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-037', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'assignment03@example.com', '010-3132-3334', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-038', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'project04@example.com', '010-4142-4344', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-039', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'revision05@example.com', '010-5152-5355', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-040', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'exam06@example.com', '010-6162-6366', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'quiz07@example.com', '010-7172-7377', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-042', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'test08@example.com', '010-8182-8388', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-043', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'studygroup09@example.com', '010-9192-9399', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-044', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'tutoring10@example.com', '010-0000-1010', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mentoring11@example.com', '010-1000-2020', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-046', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'coaching12@example.com', '010-2000-3030', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-047', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'peerreview13@example.com', '010-3000-4040', 'FEMALE', NOW(), NOW());

-- club-027 (게임동아리): 10명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-027', 'user-048', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'gamer01@example.com', '010-1113-1517', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-049', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'player02@example.com', '010-2113-2527', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-050', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'noob03@example.com', '010-3113-3537', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-001', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'pro04@example.com', '010-4113-4547', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-002', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'casual05@example.com', '010-5113-5557', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'strategist06@example.com', '010-6113-6567', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-004', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'tactician07@example.com', '010-7113-7577', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-005', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'sharpshooter08@example.com', '010-8113-8587', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-006', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'support09@example.com', '010-9113-9597', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-007', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'tank10@example.com', '010-0000-1010', 'MALE', NOW(), NOW());

-- club-028 (등산동아리): 1명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-028', 'user-008', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'climber01@example.com', '010-1114-1618', 'FEMALE', NOW(), NOW());

-- club-029 (기독교동아리): 9명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-029', 'user-009', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'christian01@example.com', '010-1115-1617', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-010', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'bible02@example.com', '010-2115-2628', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-011', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'faith03@example.com', '010-3115-3638', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-012', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'hope04@example.com', '010-4115-4648', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-013', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'love05@example.com', '010-5115-5658', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-014', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'grace06@example.com', '010-6115-6768', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-015', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'peace07@example.com', '010-7115-7878', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-016', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'joy08@example.com', '010-8115-8989', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-017', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'blessing09@example.com', '010-9192-9090', 'FEMALE', NOW(), NOW());

-- club-030 (자유동아리): 11명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-030', 'user-018', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'freedom01@example.com', '010-1116-1718', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-019', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'liberty02@example.com', '010-2116-2728', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-020', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'independence03@example.com', '010-3116-3738', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-021', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'selfdetermination04@example.com', '010-4116-4748', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-022', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'autonomy05@example.com', '010-5116-5758', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-023', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'sovereignty06@example.com', '010-6116-6768', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-024', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'liberation07@example.com', '010-7116-7778', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-025', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'emancipation08@example.com', '010-8116-8788', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-026', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'selfgovernance09@example.com', '010-9192-9797', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-027', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'personality10@example.com', '010-0000-0808', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-028', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'individuality11@example.com', '010-1000-1818', 'FEMALE', NOW(), NOW());

-- club-031 (벽외조사 동아리): 5명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-031', 'user-029', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'investigator01@example.com', '010-1117-1819', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-031', 'user-030', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'detective02@example.com', '010-2117-2829', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-031', 'user-031', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'researcher03@example.com', '010-3117-3839', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-031', 'user-032', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'explorer04@example.com', '010-4117-4849', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-031', 'user-033', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'scout05@example.com', '010-5117-5859', 'MALE', NOW(), NOW());

-- club-032 (기계공학연구회): 8명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-032', 'user-034', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'mechanical01@example.com', '010-1118-1920', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-035', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical02@example.com', '010-2118-2930', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-036', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical03@example.com', '010-3118-3940', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-037', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical04@example.com', '010-4118-4950', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-038', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical05@example.com', '010-5118-5960', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-039', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical06@example.com', '010-6118-6970', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-040', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical07@example.com', '010-7118-7980', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-032', 'user-041', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'mechanical08@example.com', '010-8118-8990', 'FEMALE', NOW(), NOW());

-- club-033 (전자공학동아리): 10명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-033', 'user-042', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'electronics01@example.com', '010-1119-2021', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-043', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics02@example.com', '010-2119-3031', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-044', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics03@example.com', '010-3119-4041', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-045', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics04@example.com', '010-4119-5051', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-046', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics05@example.com', '010-5119-6061', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-047', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics06@example.com', '010-6119-7071', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-048', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics07@example.com', '010-7119-8081', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-049', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics08@example.com', '010-8119-9091', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-050', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics09@example.com', '010-9119-0001', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-033', 'user-001', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'electronics10@example.com', '010-0119-1011', 'FEMALE', NOW(), NOW());

-- club-034 (화학공학연구회): 6명
INSERT INTO club_members (id, club_id, member_id, grade, major, role, email, phone_number, gender, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-034', 'user-002', 'FIRST_GRADE', 'Undeclared', 'PRESIDENT', 'chemical01@example.com', '010-1120-2122', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-034', 'user-003', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemical02@example.com', '010-2120-3132', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-034', 'user-004', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemical03@example.com', '010-3120-4142', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-034', 'user-005', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemical04@example.com', '010-4120-5152', 'MALE', NOW(), NOW()),
(gen_random_uuid(), 'club-034', 'user-006', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemical05@example.com', '010-5120-6162', 'FEMALE', NOW(), NOW()),
(gen_random_uuid(), 'club-034', 'user-007', 'FIRST_GRADE', 'Undeclared', 'MEMBER', 'chemical06@example.com', '010-6120-7172', 'MALE', NOW(), NOW());

