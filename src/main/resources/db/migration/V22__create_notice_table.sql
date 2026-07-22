-- 서비스 전역 공지사항 테이블 (특정 동아리에 종속되지 않음, 텍스트 전용)
CREATE TABLE notices
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    view_count INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 목록 조회 정렬(createdAt DESC, id DESC) 및 페이지네이션 성능을 위한 인덱스
CREATE INDEX idx_notices_created_at_id ON notices (created_at DESC, id DESC);
