-- FCM 토큰 테이블에 복합 유니크 제약 조건 추가
-- 기존 token unique 제약 조건 제거 후 (email, device_type) 복합 유니크 제약 조건 추가

-- 1. 기존 token unique 제약 조건 제거
ALTER TABLE fcm_token DROP CONSTRAINT uk_fcm_token_token;

-- 2. 복합 유니크 제약 조건 추가
ALTER TABLE fcm_token
ADD CONSTRAINT uk_fcm_token_email_device
UNIQUE (email, device_type);

-- 3. 성능 최적화를 위한 인덱스 추가
CREATE INDEX idx_fcm_token_email ON fcm_token (email);
CREATE INDEX idx_fcm_token_device_type ON fcm_token (device_type);
