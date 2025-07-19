INSERT INTO applyforms (id, title, sub_title, status, apply_start_date, apply_end_date, 
                                       has_interview, max_apply_count, club_id, form_json, created_at, updated_at) 
                                 VALUES (?, '그림사랑 신입 모집 테스트', '미술을 사랑하는 분들을 찾습니다', 'ACTIVE', 
                        '2025-03-01', '2025-03-15', false, 10, 'club-001', '[]', NOW(), NOW());