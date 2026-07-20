-- 서비스 운영/유지보수 팀(ROLE_SUPER_ADMIN) 계정 테이블
CREATE TABLE super_admins
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_super_admins_username UNIQUE (username)
);

-- 운영자 시드 계정 1개.
-- 비밀번호 해시는 기존 mock admin 시드와 동일한 값이므로, 로그인 비밀번호도 mock admin 계정과 동일합니다.
-- (updated_at 은 JPA Auditing(BaseTimeEntity)이 애플리케이션 레벨에서 갱신합니다.)
INSERT INTO super_admins (id, username, password, created_at, updated_at) VALUES
('11111111-1111-4111-8111-111111111111', 'ttok_operator', '$2a$10$HwEfHk9L3AqdL2zhRzLi7e7g/fSdNNgOH/fbrCwdan/Ed.5su7gRC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
