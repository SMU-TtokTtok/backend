CREATE TABLE fcm_token
(
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    device_type VARCHAR(20)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_fcm_token_token UNIQUE (token)
);

-- 디바이스 타입 제약 조건 추가
ALTER TABLE fcm_token
    ADD CONSTRAINT chk_device_type
        CHECK (device_type IN ('WEB', 'ANDROID', 'IOS', 'UNKNOWN'));

-- updated_at 자동 업데이트를 위한 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- fcm_token 테이블에 updated_at 자동 업데이트 트리거 추가
CREATE TRIGGER update_fcm_token_updated_at
    BEFORE UPDATE ON fcm_token
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

