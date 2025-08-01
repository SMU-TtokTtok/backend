-- 15. DocumentPhase 데이터 (모든 지원자는 서류 단계를 거침)
INSERT INTO document_phases (
    id, applicant_id, answers, status, created_at, updated_at
) VALUES

-- 그림사랑 동아리 지원자들
('doc-phase-001', 'applicant-001',
 '[{"title":"자기소개","subTitle":"간단한 자기소개를 작성해주세요","questionType":"SHORT_ANSWER","isEssential":true,"content":[],"answer":"안녕하세요! 미술을 사랑하는 김지영입니다. 어릴 때부터 그림 그리는 것을 좋아했고, 대학에서 미술학과에 진학하게 되었습니다."},{"title":"지원동기","subTitle":"동아리에 지원하는 이유를 작성해주세요","questionType":"LONG_ANSWER","isEssential":true,"content":[],"answer":"그림사랑 동아리에서 다양한 미술 기법을 배우고 싶고, 같은 관심사를 가진 사람들과 함께 작품 활동을 하고 싶어서 지원하게 되었습니다. 특히 전시회 개최 경험을 쌓고 싶습니다."}]',
 'EVALUATING', NOW(), NOW()),

('doc-phase-002', 'applicant-002',
 '[{"title":"자기소개","subTitle":"간단한 자기소개를 작성해주세요","questionType":"SHORT_ANSWER","isEssential":true,"content":[],"answer":"디자인학과 3학년 박민수입니다. 평소 드로잉과 디지털 아트에 관심이 많습니다."},{"title":"지원동기","subTitle":"동아리에 지원하는 이유를 작성해주세요","questionType":"LONG_ANSWER","isEssential":true,"content":[],"answer":"디자인 전공자로서 전통적인 미술 기법도 배워보고 싶고, 다양한 장르의 예술 작품을 접해보고 싶어서 지원했습니다."}]',
 'PASS', NOW(), NOW()),

('doc-phase-003', 'applicant-003',
 '[{"title":"자기소개","subTitle":"간단한 자기소개를 작성해주세요","questionType":"SHORT_ANSWER","isEssential":true,"content":[],"answer":"회화과 1학년 이수진입니다."},{"title":"지원동기","subTitle":"동아리에 지원하는 이유를 작성해주세요","questionType":"LONG_ANSWER","isEssential":true,"content":[],"answer":"동아리 활동을 통해 실력을 늘리고 싶습니다."}]',
 'FAIL', NOW(), NOW()),

-- 상명축구부 지원자들
('doc-phase-004', 'applicant-004',
 '[{"title":"포지션","subTitle":"선호하는 포지션을 선택해주세요","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"],"answer":"미드필더"},{"title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"고등학교 때 축구부에서 3년간 활동했습니다. 주로 중앙 미드필더 포지션을 맡았고, 지역 대회에서 우승 경험이 있습니다."}]',
 'EVALUATING', NOW(), NOW()),

('doc-phase-005', 'applicant-005',
 '[{"title":"포지션","subTitle":"선호하는 포지션을 선택해주세요","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"],"answer":"공격수"},{"title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"중학교부터 축구를 시작했고, 고등학교에서는 학교 대표로 각종 대회에 참가했습니다. 골 결정력이 좋다는 평가를 받고 있습니다."}]',
 'PASS', NOW(), NOW()),

('doc-phase-006', 'applicant-006',
 '[{"title":"포지션","subTitle":"선호하는 포지션을 선택해주세요","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"],"answer":"수비수"},{"title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"대학교 입학 후부터 축구를 본격적으로 시작했습니다. 늦게 시작했지만 열정적으로 훈련에 참여하고 있고, 수비 센스가 좋다는 평가를 받고 있습니다."}]',
 'PASS', NOW(), NOW()),

('doc-phase-007', 'applicant-007',
 '[{"title":"포지션","subTitle":"선호하는 포지션을 선택해주세요","questionType":"RADIO","isEssential":true,"content":["공격수","미드필더","수비수","골키퍼"],"answer":"골키퍼"},{"title":"축구 경력","subTitle":"축구 경험에 대해 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"골키퍼 포지션에 관심이 있어서 개인적으로 연습하고 있습니다. 반응속도가 빠른 편이고, 골키퍼로서 성장하고 싶습니다."}]',
 'PASS', NOW(), NOW()),

-- 코딩마스터 지원자들
('doc-phase-008', 'applicant-008',
 '[{"title":"프로그래밍 언어","subTitle":"사용 가능한 프로그래밍 언어를 모두 선택해주세요","questionType":"CHECKBOX","isEssential":true,"content":["Java","Python","JavaScript","C++","Go"],"answer":["Java","Python"]},{"title":"개발 경험","subTitle":"개발 프로젝트 경험을 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"학교 과제로 간단한 웹 프로젝트를 진행해본 경험이 있습니다. Spring Boot와 React를 사용했고, 팀 프로젝트에서 백엔드 개발을 담당했습니다."}]',
 'EVALUATING', NOW(), NOW()),

('doc-phase-009', 'applicant-009',
 '[{"title":"프로그래밍 언어","subTitle":"사용 가능한 프로그래밍 언어를 모두 선택해주세요","questionType":"CHECKBOX","isEssential":true,"content":["Java","Python","JavaScript","C++","Go"],"answer":["Java","JavaScript","Python"]},{"title":"개발 경험","subTitle":"개발 프로젝트 경험을 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"개인 프로젝트로 To-Do 앱을 개발해봤고, 오픈소스 프로젝트에도 기여한 경험이 있습니다. 알고리즘 문제 해결에도 관심이 많아서 코딩테스트 준비도 하고 있습니다."}]',
 'PASS', NOW(), NOW()),

('doc-phase-010', 'applicant-010',
 '[{"title":"프로그래밍 언어","subTitle":"사용 가능한 프로그래밍 언어를 모두 선택해주세요","questionType":"CHECKBOX","isEssential":true,"content":["Java","Python","JavaScript","C++","Go"],"answer":["Python"]},{"title":"개발 경험","subTitle":"개발 프로젝트 경험을 작성해주세요","questionType":"LONG_ANSWER","isEssential":false,"content":[],"answer":"Python으로 간단한 스크립트 작성 경험이 있습니다."}]',
 'FAIL', NOW(), NOW()),

-- 나눔봉사단 지원자들 (빈 form_json이므로 빈 답변)
('doc-phase-011', 'applicant-011', '[]', 'EVALUATING', NOW(), NOW()),
('doc-phase-012', 'applicant-012', '[]', 'PASS', NOW(), NOW()),

-- 농구동아리 지원자들 (빈 form_json이므로 빈 답변)
('doc-phase-013', 'applicant-013', '[]', 'PASS', NOW(), NOW()),
('doc-phase-014', 'applicant-014', '[]', 'PASS', NOW(), NOW()),

-- 영어회화 지원자들 (빈 form_json이므로 빈 답변)
('doc-phase-015', 'applicant-015', '[]', 'EVALUATING', NOW(), NOW()),

-- 댄스크루 지원자들 (빈 form_json이므로 빈 답변)
('doc-phase-016', 'applicant-016', '[]', 'PASS', NOW(), NOW()),
('doc-phase-017', 'applicant-017', '[]', 'PASS', NOW(), NOW());
