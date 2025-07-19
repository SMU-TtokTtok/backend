-- 5. 즐겨찾기 데이터 (수정된 버전)
INSERT INTO user_favorites (id, user_id, club_id, created_at, updated_at) VALUES

-- club-002 (상명축구부): 즐겨찾기 15개
(gen_random_uuid(), 'user-001', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-002', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-003', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-004', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-005', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-006', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-007', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-008', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-009', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-010', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-026', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-027', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-028', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-029', 'club-002', NOW(), NOW()),
(gen_random_uuid(), 'user-030', 'club-002', NOW(), NOW()),

-- club-003 (코딩마스터): 즐겨찾기 3개
(gen_random_uuid(), 'user-011', 'club-003', NOW(), NOW()),
(gen_random_uuid(), 'user-012', 'club-003', NOW(), NOW()),
(gen_random_uuid(), 'user-013', 'club-003', NOW(), NOW()),

-- club-001 (그림사랑): 즐겨찾기 8개
(gen_random_uuid(), 'user-014', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-015', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-016', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-017', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-018', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-031', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-032', 'club-001', NOW(), NOW()),
(gen_random_uuid(), 'user-033', 'club-001', NOW(), NOW()),

-- club-012 (댄스크루): 즐겨찾기 12개
(gen_random_uuid(), 'user-019', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-020', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-021', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-022', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-023', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-034', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-035', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-036', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-037', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-038', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-039', 'club-012', NOW(), NOW()),
(gen_random_uuid(), 'user-040', 'club-012', NOW(), NOW()),

-- club-022 (러닝크루): 즐겨찾기 4개
(gen_random_uuid(), 'user-041', 'club-022', NOW(), NOW()),
(gen_random_uuid(), 'user-042', 'club-022', NOW(), NOW()),
(gen_random_uuid(), 'user-043', 'club-022', NOW(), NOW()),
(gen_random_uuid(), 'user-044', 'club-022', NOW(), NOW()),

-- club-006 (농구동아리): 즐겨찾기 6개
(gen_random_uuid(), 'user-045', 'club-006', NOW(), NOW()),
(gen_random_uuid(), 'user-046', 'club-006', NOW(), NOW()),
(gen_random_uuid(), 'user-047', 'club-006', NOW(), NOW()),
(gen_random_uuid(), 'user-048', 'club-006', NOW(), NOW()),
(gen_random_uuid(), 'user-049', 'club-006', NOW(), NOW()),
(gen_random_uuid(), 'user-050', 'club-006', NOW(), NOW()),

-- club-010 (테니스부): 즐겨찾기 10개
(gen_random_uuid(), 'user-001', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-002', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-003', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-004', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-005', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-024', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-025', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-026', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-027', 'club-010', NOW(), NOW()),
(gen_random_uuid(), 'user-028', 'club-010', NOW(), NOW()),

-- club-004 (나눔봉사단): 즐겨찾기 2개
(gen_random_uuid(), 'user-006', 'club-004', NOW(), NOW()),
(gen_random_uuid(), 'user-007', 'club-004', NOW(), NOW()),

-- club-016 (기타동아리): 즐겨찾기 5개
(gen_random_uuid(), 'user-008', 'club-016', NOW(), NOW()),
(gen_random_uuid(), 'user-009', 'club-016', NOW(), NOW()),
(gen_random_uuid(), 'user-010', 'club-016', NOW(), NOW()),
(gen_random_uuid(), 'user-029', 'club-016', NOW(), NOW()),
(gen_random_uuid(), 'user-030', 'club-016', NOW(), NOW()),

-- club-026 (스터디모임): 즐겨찾기 7개
(gen_random_uuid(), 'user-011', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-012', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-013', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-014', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-015', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-031', 'club-026', NOW(), NOW()),
(gen_random_uuid(), 'user-032', 'club-026', NOW(), NOW()),

-- club-008 (포토클럽): 즐겨찾기 9개
(gen_random_uuid(), 'user-016', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-017', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-018', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-019', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-020', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-033', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-034', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-035', 'club-008', NOW(), NOW()),
(gen_random_uuid(), 'user-036', 'club-008', NOW(), NOW()),

-- club-019 (사랑나눔): 즐겨찾기 4개
(gen_random_uuid(), 'user-021', 'club-019', NOW(), NOW()),
(gen_random_uuid(), 'user-022', 'club-019', NOW(), NOW()),
(gen_random_uuid(), 'user-023', 'club-019', NOW(), NOW()),
(gen_random_uuid(), 'user-037', 'club-019', NOW(), NOW()),

-- club-030 (자유동아리): 즐겨찾기 6개
(gen_random_uuid(), 'user-024', 'club-030', NOW(), NOW()),
(gen_random_uuid(), 'user-025', 'club-030', NOW(), NOW()),
(gen_random_uuid(), 'user-026', 'club-030', NOW(), NOW()),
(gen_random_uuid(), 'user-038', 'club-030', NOW(), NOW()),
(gen_random_uuid(), 'user-039', 'club-030', NOW(), NOW()),
(gen_random_uuid(), 'user-040', 'club-030', NOW(), NOW()),

-- club-027 (게임동아리): 즐겨찾기 3개
(gen_random_uuid(), 'user-027', 'club-027', NOW(), NOW()),
(gen_random_uuid(), 'user-028', 'club-027', NOW(), NOW()),
(gen_random_uuid(), 'user-041', 'club-027', NOW(), NOW()),

-- club-029 (기독교동아리): 즐겨찾기 2개
(gen_random_uuid(), 'user-029', 'club-029', NOW(), NOW()),
(gen_random_uuid(), 'user-042', 'club-029', NOW(), NOW()),

-- club-025 (카페동아리): 즐겨찾기 1개
(gen_random_uuid(), 'user-030', 'club-025', NOW(), NOW()),

-- club-018 (요리연구회): 즐겨찾기 3개
(gen_random_uuid(), 'user-043', 'club-018', NOW(), NOW()),
(gen_random_uuid(), 'user-044', 'club-018', NOW(), NOW()),
(gen_random_uuid(), 'user-045', 'club-018', NOW(), NOW()),

-- club-007 (영어회화): 즐겨찾기 2개
(gen_random_uuid(), 'user-046', 'club-007', NOW(), NOW()),
(gen_random_uuid(), 'user-047', 'club-007', NOW(), NOW()),

-- club-014 (환경지키미): 즐겨찾기 3개
(gen_random_uuid(), 'user-048', 'club-014', NOW(), NOW()),
(gen_random_uuid(), 'user-049', 'club-014', NOW(), NOW()),
(gen_random_uuid(), 'user-050', 'club-014', NOW(), NOW()),

-- 나머지 동아리들은 즐겨찾기 0-1개
-- club-011 (독서모임): 즐겨찾기 1개
(gen_random_uuid(), 'user-001', 'club-011', NOW(), NOW()),

-- club-015 (영화감상): 즐겨찾기 1개
(gen_random_uuid(), 'user-002', 'club-015', NOW(), NOW()),

-- club-021 (창작문예): 즐겨찾기 1개
(gen_random_uuid(), 'user-003', 'club-021', NOW(), NOW()),

-- club-023 (과학탐구): 즐겨찾기 1개
(gen_random_uuid(), 'user-004', 'club-023', NOW(), NOW());