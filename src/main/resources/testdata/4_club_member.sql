-- 4. ClubMember 데이터 생성 (각 동아리별로 다양한 멤버 수)
-- UUID 생성을 위한 함수 사용

-- club-001 (그림사랑): 15명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-001', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-007', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-008', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-001', 'user-015', NOW(), NOW());

-- club-002 (상명축구부): 25명 (가장 인기)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-002', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-007', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-008', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-017', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-018', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-019', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-022', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-023', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-024', NOW(), NOW()),
(gen_random_uuid(), 'club-002', 'user-025', NOW(), NOW());

-- club-003 (코딩마스터): 18명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-003', 'user-026', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-028', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-029', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-030', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-031', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-032', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-033', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-034', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-035', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-036', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-037', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-038', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-039', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-040', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-041', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-042', NOW(), NOW()),
(gen_random_uuid(), 'club-003', 'user-043', NOW(), NOW());

-- club-004 (나눔봉사단): 12명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-004', 'user-044', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-045', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-046', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-047', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-048', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-049', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-050', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-004', 'user-005', NOW(), NOW());


-- club-005 (연극반): 8명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-005', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-007', NOW(), NOW()),
(gen_random_uuid(), 'club-005', 'user-008', NOW(), NOW());

-- club-006 (농구동아리): 14명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-006', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-017', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-018', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-019', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-006', 'user-022', NOW(), NOW());

-- club-007 (영어회화): 9명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-007', 'user-023', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-024', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-025', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-026', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-028', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-029', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-030', NOW(), NOW()),
(gen_random_uuid(), 'club-007', 'user-031', NOW(), NOW());

-- club-008 (포토클럽): 11명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-008', 'user-032', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-033', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-034', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-035', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-036', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-037', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-038', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-039', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-040', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-041', NOW(), NOW()),
(gen_random_uuid(), 'club-008', 'user-042', NOW(), NOW());

-- club-009 (밴드동아리): 6명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-009', 'user-043', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-044', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-045', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-046', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-047', NOW(), NOW()),
(gen_random_uuid(), 'club-009', 'user-048', NOW(), NOW());

-- club-010 (테니스부): 13명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-010', 'user-049', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-050', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-007', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-008', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-010', 'user-011', NOW(), NOW());

-- club-011 (독서모임): 7명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-011', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-017', NOW(), NOW()),
(gen_random_uuid(), 'club-011', 'user-018', NOW(), NOW());

-- club-012 (댄스크루): 16명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-012', 'user-019', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-022', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-023', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-024', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-025', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-026', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-028', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-029', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-030', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-031', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-032', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-033', NOW(), NOW()),
(gen_random_uuid(), 'club-012', 'user-034', NOW(), NOW());

-- club-013 (토론동아리): 5명 (모집마감)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-013', 'user-035', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-036', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-037', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-038', NOW(), NOW()),
(gen_random_uuid(), 'club-013', 'user-039', NOW(), NOW());

-- club-014 (환경지키미): 10명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-014', 'user-040', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-041', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-042', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-043', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-044', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-045', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-046', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-047', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-048', NOW(), NOW()),
(gen_random_uuid(), 'club-014', 'user-049', NOW(), NOW());

-- club-015 (영화감상): 8명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-015', 'user-050', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-015', 'user-007', NOW(), NOW());

-- club-016 (기타동아리): 12명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-016', 'user-008', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-017', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-018', NOW(), NOW()),
(gen_random_uuid(), 'club-016', 'user-019', NOW(), NOW());

-- club-017 (배드민턴): 4명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-017', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-022', NOW(), NOW()),
(gen_random_uuid(), 'club-017', 'user-023', NOW(), NOW());

-- club-018 (요리연구회): 9명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-018', 'user-024', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-025', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-026', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-028', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-029', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-030', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-031', NOW(), NOW()),
(gen_random_uuid(), 'club-018', 'user-032', NOW(), NOW());

-- club-019 (사랑나눔): 11명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-019', 'user-033', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-034', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-035', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-036', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-037', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-038', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-039', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-040', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-041', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-042', NOW(), NOW()),
(gen_random_uuid(), 'club-019', 'user-043', NOW(), NOW());

-- club-020 (체스동아리): 3명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-020', 'user-044', NOW(), NOW()),
(gen_random_uuid(), 'club-020', 'user-045', NOW(), NOW()),
(gen_random_uuid(), 'club-020', 'user-046', NOW(), NOW());

-- club-021 (창작문예): 7명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-021', 'user-047', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-048', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-049', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-050', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-021', 'user-003', NOW(), NOW());

-- club-022 (러닝크루): 15명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-022', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-007', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-008', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-017', NOW(), NOW()),
(gen_random_uuid(), 'club-022', 'user-018', NOW(), NOW());

-- club-023 (과학탐구): 6명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-023', 'user-019', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-022', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-023', NOW(), NOW()),
(gen_random_uuid(), 'club-023', 'user-024', NOW(), NOW());

-- club-024 (여행동아리): 2명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-024', 'user-025', NOW(), NOW()),
(gen_random_uuid(), 'club-024', 'user-026', NOW(), NOW());

-- club-025 (카페동아리): 8명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-025', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-028', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-029', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-030', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-031', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-032', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-033', NOW(), NOW()),
(gen_random_uuid(), 'club-025', 'user-034', NOW(), NOW());

-- club-026 (스터디모임): 13명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-026', 'user-035', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-036', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-037', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-038', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-039', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-040', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-041', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-042', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-043', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-044', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-045', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-046', NOW(), NOW()),
(gen_random_uuid(), 'club-026', 'user-047', NOW(), NOW());

-- club-027 (게임동아리): 10명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-027', 'user-048', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-049', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-050', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-001', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-002', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-003', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-004', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-005', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-006', NOW(), NOW()),
(gen_random_uuid(), 'club-027', 'user-007', NOW(), NOW());

-- club-028 (등산동아리): 1명 (모집마감, 인기도 낮음)
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-028', 'user-008', NOW(), NOW());

-- club-029 (기독교동아리): 9명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-029', 'user-009', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-010', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-011', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-012', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-013', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-014', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-015', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-016', NOW(), NOW()),
(gen_random_uuid(), 'club-029', 'user-017', NOW(), NOW());

-- club-030 (자유동아리): 11명
INSERT INTO club_members (id, club_id, member_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'club-030', 'user-018', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-019', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-020', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-021', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-022', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-023', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-024', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-025', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-026', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-027', NOW(), NOW()),
(gen_random_uuid(), 'club-030', 'user-028', NOW(), NOW());
-- (더 많이 만들 수도 있지만 우선 이 4개로 테스트)