CREATE TABLE fcm_token
(
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    device_type VARCHAR(20)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 디바이스 타입 제약 조건 추가
ALTER TABLE fcm_token
    ADD CONSTRAINT chk_device_type
        CHECK (device_type IN ('WEB', 'ANDROID', 'IOS', 'UNKNOWN'));
