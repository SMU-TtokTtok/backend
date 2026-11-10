package org.project.ttokttok.domain.notification.mail.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
}
