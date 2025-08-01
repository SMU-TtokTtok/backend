-- 마감된 모집 (INACTIVE 상태) - recruiting: false가 되어야 함
INSERT INTO applyforms (id, title, sub_title, status, apply_start_date, apply_end_date, 
                                       has_interview, max_apply_count, club_id, form_json, created_at, updated_at) 
VALUES 
(gen_random_uuid(), '연극반 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 5, 'club-005', '[]', NOW(), NOW()),

(gen_random_uuid(), '벽외조사 동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 5, 'club-031', '[]', NOW(), NOW()),

(gen_random_uuid(), '자유동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 11, 'club-030', '[]', NOW(), NOW()),

(gen_random_uuid(), '기독교동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 9, 'club-029', '[]', NOW(), NOW()),

(gen_random_uuid(), '등산동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 1, 'club-028', '[]', NOW(), NOW()),

(gen_random_uuid(), '게임동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 10, 'club-027', '[]', NOW(), NOW()),

(gen_random_uuid(), '스터디모임 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 13, 'club-026', '[]', NOW(), NOW()),

(gen_random_uuid(), '카페동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 8, 'club-025', '[]', NOW(), NOW()),

(gen_random_uuid(), '여행동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 2, 'club-024', '[]', NOW(), NOW()),

(gen_random_uuid(), '과학탐구 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 6, 'club-023', '[]', NOW(), NOW()),

(gen_random_uuid(), '러닝크루 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 15, 'club-022', '[]', NOW(), NOW()),

(gen_random_uuid(), '창작문예 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 7, 'club-021', '[]', NOW(), NOW()),

(gen_random_uuid(), '체스동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 3, 'club-020', '[]', NOW(), NOW()),

(gen_random_uuid(), '사랑나눔 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 11, 'club-019', '[]', NOW(), NOW()),

(gen_random_uuid(), '요리연구회 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 9, 'club-018', '[]', NOW(), NOW()),

(gen_random_uuid(), '배드민턴 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 4, 'club-017', '[]', NOW(), NOW()),

(gen_random_uuid(), '기타동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 11, 'club-016', '[]', NOW(), NOW()),

(gen_random_uuid(), '영화감상 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 8, 'club-015', '[]', NOW(), NOW()),

(gen_random_uuid(), '환경지키미 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 11, 'club-014', '[]', NOW(), NOW()),

(gen_random_uuid(), '토론동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
 '2025-02-01', '2025-02-15', false, 5, 'club-013', '[]', NOW(), NOW()),

(gen_random_uuid(), '밴드동아리 모집 마감', '모집이 마감되었습니다', 'INACTIVE',
 '2025-02-01', '2025-02-15', false, 6, 'club-009', '[]', NOW(), NOW());