-- admin 테이블에 email 컬럼 추가 (유니크 제약조건 포함)
-- 기존 데이터가 있을 경우 빈 문자열을 기본값으로 설정
ALTER TABLE admin
ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '' UNIQUE;