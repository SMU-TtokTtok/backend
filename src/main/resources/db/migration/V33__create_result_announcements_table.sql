-- #465: 결과 발표 기록. 확정과 발송 작업 저장을 한 트랜잭션으로 묶은 발표 API가 쓴다.
-- 같은 동아리·같은 Idempotency-Key는 한 번만, 같은 지원 폼·전형도 한 번만 발표된다.
-- 새 테이블만 추가하므로 이전 버전 코드와 함께 돌아도 안전하다.
CREATE TABLE result_announcements
(
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    club_id             VARCHAR(36)  NOT NULL REFERENCES clubs (id) ON DELETE CASCADE,
    apply_form_id       VARCHAR(36)  NOT NULL REFERENCES applyforms (id) ON DELETE CASCADE,
    phase               VARCHAR(20)  NOT NULL,
    idempotency_key     VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64)  NOT NULL,
    pass_mail_count     INTEGER      NOT NULL,
    fail_mail_count     INTEGER      NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_result_announcements_club_key UNIQUE (club_id, idempotency_key),
    CONSTRAINT uk_result_announcements_form_phase UNIQUE (apply_form_id, phase),
    CONSTRAINT chk_result_announcements_phase CHECK (phase IN ('DOCUMENT', 'INTERVIEW'))
);
