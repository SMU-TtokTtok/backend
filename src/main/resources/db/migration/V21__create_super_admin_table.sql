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

-- 운영자 계정은 자격증명을 저장소(git)에 커밋하지 않기 위해 여기서 시드하지 않는다.
-- 앱 시작 시 SUPER_ADMIN_USERNAME / SUPER_ADMIN_PASSWORD 환경변수(시크릿)를 읽어
-- SuperAdminBootstrap 이 계정이 없을 때만 멱등적으로 생성한다.
