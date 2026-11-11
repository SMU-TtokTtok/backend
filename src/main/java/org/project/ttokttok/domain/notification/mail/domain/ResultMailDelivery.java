package org.project.ttokttok.domain.notification.mail.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ttokttok.global.entity.BaseTimeEntity;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

/**
 * 결과 메일 한 통의 발송 작업. 수신자 한 명당 한 행이다.
 */
@Entity
@Getter
@Table(name = "result_mail_deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResultMailDelivery extends BaseTimeEntity {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LEASE = Duration.ofMinutes(5);
    static final Duration RETRY_BASE = Duration.ofMinutes(1);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, updatable = false)
    private String id;

    @Column(name = "request_id", nullable = false, length = 36, updatable = false)
    private String requestId;

    @Column(name = "club_id", nullable = false, length = 36, updatable = false)
    private String clubId;

    @Column(name = "recipient_email", nullable = false, updatable = false)
    private String recipientEmail;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String subject;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultMailDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public static ResultMailDelivery pending(String requestId, String clubId, String recipientEmail,
                                             ResultMailContent content, LocalDateTime now) {
        ResultMailDelivery delivery = new ResultMailDelivery();
        delivery.requestId = requestId;
        delivery.clubId = clubId;
        delivery.recipientEmail = recipientEmail;
        delivery.subject = content.title();
        delivery.body = content.body();
        delivery.status = ResultMailDeliveryStatus.PENDING;
        delivery.attemptCount = 0;
        delivery.nextAttemptAt = now;
        return delivery;
    }

    public boolean isSending() {
        return status == ResultMailDeliveryStatus.SENDING;
    }

    public boolean hasAttemptsLeft() {
        return attemptCount < MAX_ATTEMPTS;
    }

    public void startAttempt(LocalDateTime now) {
        status = ResultMailDeliveryStatus.SENDING;
        attemptCount++;
        leaseUntil = now.plus(LEASE);
    }

    public void markSent(LocalDateTime now) {
        status = ResultMailDeliveryStatus.SENT;
        sentAt = now;
        leaseUntil = null;
    }

    // 일시 실패이고 시도가 남았으면 RETRY_BASE × 2^(attemptCount-1) 뒤로 다시 미룬다. 아니면 끝낸다.
    public void markFailed(String error, boolean retryable, LocalDateTime now) {
        lastError = error;
        leaseUntil = null;
        if (retryable && hasAttemptsLeft()) {
            status = ResultMailDeliveryStatus.PENDING;
            nextAttemptAt = now.plus(RETRY_BASE.multipliedBy(1L << Math.max(0, attemptCount - 1)));
            return;
        }
        status = ResultMailDeliveryStatus.FAILED;
    }

    public ResultMailContent content() {
        return new ResultMailContent(subject, body);
    }
}
