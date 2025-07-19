-- 마감된 모집 (INACTIVE 상태) - recruiting: false가 되어야 함
INSERT INTO applyforms (id, title, sub_title, status, apply_start_date, apply_end_date, 
                                       has_interview, max_apply_count, club_id, form_json, created_at, updated_at) 
VALUES (?, '연극반 모집 마감', '모집이 마감되었습니다', 'INACTIVE', 
        '2025-02-01', '2025-02-15', false, 5, 'club-005', '[]', NOW(), NOW());