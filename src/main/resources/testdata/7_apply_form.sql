-- 7. 지원폼 데이터 (주요 동아리들만)
INSERT INTO applyforms (id, title, sub_title, status, apply_start_date, apply_end_date,
                        has_interview, max_apply_count, club_id, form_json, created_at, updated_at)
VALUES

-- 모집중인 동아리들 (ACTIVE enum 값 사용)
(gen_random_uuid(), '그림사랑 신입 모집', '미술을 사랑하는 분들을 찾습니다', 'ACTIVE',
 '2025-03-01', '2025-03-15', false, 10, 'club-001',
 '[{"questionId":"q1","title":"자기소개","subTitle":"간단한 자기소개를 작성해주세요","questionType":"SHORT_ANSWER","isEssential":true,"content":[]},{"questionId":"q2","title":"지원동기","subTitle":"동아리에 지원하는 이유를 작성해주세요","questionType":"LONG_ANSWER","isEssential":true,"content":[]}]',
 NOW(), NOW()),

(gen_random_uuid(), '예전 상명축구부 모집', '함께 안 뛸 선수를 찾습니다', 'INACTIVE',
 '2024-03-01', '2025-03-20', true, 5, 'club-002',
 '[{"questionId":"q3","title":"예에에에전 지원폼","subTitle":"예전 지원폼","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"]},{"questionId":"q4","title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[]}]',
 NOW(), NOW()),

(gen_random_uuid(), '상명축구부 모집', '함께 뛸 선수를 찾습니다', 'ACTIVE',
 '2025-03-01', '2025-03-20', true, 15, 'club-002',
 '[{"questionId":"q3","title":"포지션","subTitle":"선호하는 포지션을 선택해주세요","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"]},{"questionId":"q4","title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[]}]',
 NOW(), NOW()),

(gen_random_uuid(), '코딩마스터 모집', '프로그래밍 실력을 키워요', 'ACTIVE',
 '2025-03-05', '2025-03-25', false, 12, 'club-003',
 '[{"questionId":"q5","title":"프로그래밍 언어","subTitle":"사용 가능한 프로그래밍 언어를 모두 선택해주세요","questionType":"CHECKBOX","isEssential":true,"content":["Java","Python","JavaScript","C++","Go"]},{"questionId":"q6","title":"개발 경험","subTitle":"개발 프로젝트 경험을 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[]}]',
 NOW(), NOW()),

(gen_random_uuid(), '나눔봉사단 모집', '함께 봉사해요', 'ACTIVE',
 '2025-03-01', '2025-03-30', false, 20, 'club-004', '[]', NOW(), NOW()),

(gen_random_uuid(), '농구동아리 모집', '농구 좋아하세요?', 'ACTIVE',
 '2025-03-10', '2025-03-25', true, 8, 'club-006', '[]', NOW(), NOW()),

(gen_random_uuid(), '영어회화 모집', '영어 실력 향상하기', 'ACTIVE',
 '2025-03-01', '2025-03-15', false, 15, 'club-007', '[]', NOW(), NOW()),

(gen_random_uuid(), '포토클럽 모집', '사진 찍는 재미를 느껴보세요', 'ACTIVE',
 '2025-03-05', '2025-03-20', false, 10, 'club-008', '[]', NOW(), NOW()),

(gen_random_uuid(), '테니스부 모집', '테니스로 건강하게', 'ACTIVE',
 '2025-03-01', '2025-03-18', false, 12, 'club-010', '[]', NOW(), NOW()),

(gen_random_uuid(), '독서모임 모집', '책을 통한 성장', 'ACTIVE',
 '2025-03-01', '2025-03-12', false, 8, 'club-011', '[]', NOW(), NOW()),

(gen_random_uuid(), '댄스크루 모집', '춤으로 하나되어요', 'ACTIVE',
 '2025-03-01', '2025-03-22', true, 15, 'club-012', '[]', NOW(), NOW()),

-- ENGINEERING 과동아리 모집 폼 추가 (과동아리 필터링 테스트용)
(gen_random_uuid(), '기계공학연구회 모집', '기계공학에 관심있는 분들', 'ACTIVE',
 '2025-03-01', '2025-03-20', false, 10, 'club-032', '[]', NOW(), NOW()),

(gen_random_uuid(), '전자공학동아리 모집', '전자공학 실습 함께해요', 'ACTIVE',
 '2025-03-05', '2025-03-25', true, 12, 'club-033', '[]', NOW(), NOW()),

(gen_random_uuid(), '화학공학연구회 모집', '화학공학 실험 연구', 'ACTIVE',
 '2025-03-01', '2025-03-18', false, 8, 'club-034', '[]', NOW(), NOW());

INSERT INTO applyforms (id,
                       title,
                       sub_title,
                       status,
                       apply_start_date,
                       apply_end_date,
                       has_interview,
                       max_apply_count,
                       club_id,
                       form_json,
                       created_at,
                       updated_at)
VALUES ('2',
        '멋쟁이 사자처럼 지원서',
        '아래 문항들을 모두 작성해주세요.',
        'ACTIVE',
        '2024-08-05',
        '2024-08-20',
        true,
        15,
        'club-035',
        '[
            {
                "questionId": "1",
                "title": "성함을 입력해주세요.",
                "subTitle": "본명을 기입해주세요.",
                "questionType": "SHORT_ANSWER",
                "isEssential": true,
                "content": null
            },
            {
                "questionId": "2",
                "title": "자기소개를 작성해주세요.",
                "subTitle": "경험, 장점, 관심사를 중심으로 작성해주세요.",
                "questionType": "LONG_ANSWER",
                "isEssential": true,
                "content": null
            },
            {
                "questionId": "3",
                "title": "지원하게 된 동기를 선택해주세요.",
                "subTitle": "",
                "questionType": "CHECKBOX",
                "isEssential": true,
                "content": ["관심 있는 분야라서", "지인의 추천", "학점과 관련", "기타"]
            },
            {
                "questionId": "4",
                "title": "성별을 선택해주세요.",
                "subTitle": "",
                "questionType": "RADIO",
                "isEssential": true,
                "content": ["남성", "여성", "기타", "응답하지 않음"]
            },
            {
                "questionId": "5",
                "title": "이력서를 업로드해주세요.",
                "subTitle": "PDF 형식 권장",
                "questionType": "FILE",
                "isEssential": true,
                "content": null
            }
        ]',
       now(), now());
