-- 게시판 인스타그램형 피드: 대표(썸네일) 이미지 URL 컬럼 추가
-- 기존 텍스트 게시글은 NULL 유지 (신규 게시글부터 애플리케이션 레벨에서 필수)
ALTER TABLE club_boards ADD COLUMN thumbnail_url VARCHAR(512);
