-- 2학년만 모집하는 특별한 케이스
INSERT INTO applyforms (id, title, sub_title, status, apply_start_date, apply_end_date, 
                                       has_interview, max_apply_count, club_id, form_json, created_at, updated_at) 
VALUES (?, '밴드동아리 2학년 특별모집', '2학년만 특별 모집합니다', 'ACTIVE', 
        '2025-03-01', '2025-03-20', true, 3, 'club-009', '[]', NOW(), NOW());