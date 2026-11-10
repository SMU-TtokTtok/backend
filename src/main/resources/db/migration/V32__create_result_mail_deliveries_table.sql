-- #460: 결과 메일을 수신자별 발송 작업으로 저장한다. 워커가 트랜잭션 밖에서 읽어 보낸다.
-- 새 테이블만 추가하므로 이전 버전 코드와 함께 돌아도 안전하다.
CREATE TABLE result_mail_deliveries
(
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    request_id      VARCHAR(36)  NOT NULL,
    club_id         VARCHAR(36)  NOT NULL REFERENCES clubs (id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP    NOT NULL,
    lease_until     TIMESTAMP,
    last_error      VARCHAR(255),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_result_mail_deliveries_status
        CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_result_mail_deliveries_status_next_attempt
    ON result_mail_deliveries (status, next_attempt_at);

CREATE INDEX idx_result_mail_deliveries_request
    ON result_mail_deliveries (request_id);
