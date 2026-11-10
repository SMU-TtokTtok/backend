package org.project.ttokttok.domain.notification.mail.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;

class ResultMailDeliveryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 10, 0);
    private static final ResultMailContent CONTENT = new ResultMailContent("합격", "축하합니다");

    private ResultMailDelivery pending() {
        return ResultMailDelivery.pending("req-1", "club-1", "a@example.com", CONTENT, NOW);
    }

    @Test
    @DisplayName("pending(): PENDING 상태, 시도 0회, 바로 발송 가능한 시각으로 만든다")
    void pending_initialState() {
        ResultMailDelivery delivery = pending();

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.PENDING);
        assertThat(delivery.getAttemptCount()).isZero();
        assertThat(delivery.getNextAttemptAt()).isEqualTo(NOW);
        assertThat(delivery.getRequestId()).isEqualTo("req-1");
        assertThat(delivery.getClubId()).isEqualTo("club-1");
        assertThat(delivery.getRecipientEmail()).isEqualTo("a@example.com");
        assertThat(delivery.content()).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("startAttempt(): SENDING, 시도 1회, 임대는 5분 뒤까지")
    void startAttempt_leasesFiveMinutes() {
        ResultMailDelivery delivery = pending();

        delivery.startAttempt(NOW);

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.SENDING);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getLeaseUntil()).isEqualTo(NOW.plusMinutes(5));
    }

    @Test
    @DisplayName("markSent(): SENT, 발송 시각을 남기고 임대를 비운다")
    void markSent_clearsLease() {
        ResultMailDelivery delivery = pending();
        delivery.startAttempt(NOW);

        delivery.markSent(NOW.plusSeconds(3));

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.SENT);
        assertThat(delivery.getSentAt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(delivery.getLeaseUntil()).isNull();
    }

    private ResultMailDelivery afterAttempts(int attempts) {
        ResultMailDelivery delivery = pending();
        for (int i = 0; i < attempts; i++) {
            delivery.startAttempt(NOW);
        }
        return delivery;
    }

    @ParameterizedTest(name = "{0}회 실패 뒤 재시도는 {1}분 뒤")
    @CsvSource({"1,1", "2,2", "3,4", "4,8"})
    @DisplayName("markFailed(retryable): 시도가 남았으면 PENDING으로 두고 1·2·4·8분 뒤로 미룬다")
    void markFailed_retryable_backsOffExponentially(int attempts, long minutes) {
        ResultMailDelivery delivery = afterAttempts(attempts);
        LocalDateTime failedAt = NOW.plusSeconds(30);

        delivery.markFailed("X", true, failedAt);

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.PENDING);
        assertThat(delivery.getNextAttemptAt()).isEqualTo(failedAt.plusMinutes(minutes));
        assertThat(delivery.getLastError()).isEqualTo("X");
        assertThat(delivery.getLeaseUntil()).isNull();
    }

    @Test
    @DisplayName("markFailed(retryable): 5번째 시도가 실패하면 FAILED")
    void markFailed_retryable_failsAfterMaxAttempts() {
        ResultMailDelivery delivery = afterAttempts(5);

        delivery.markFailed("X", true, NOW);

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.FAILED);
        assertThat(delivery.hasAttemptsLeft()).isFalse();
    }

    @Test
    @DisplayName("markFailed(영구 실패): 시도가 남아 있어도 FAILED")
    void markFailed_notRetryable_failsImmediately() {
        ResultMailDelivery delivery = afterAttempts(1);

        delivery.markFailed("X", false, NOW);

        assertThat(delivery.getStatus()).isEqualTo(ResultMailDeliveryStatus.FAILED);
        assertThat(delivery.getLastError()).isEqualTo("X");
    }
}
