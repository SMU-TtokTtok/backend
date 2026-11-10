package org.project.ttokttok.domain.notification.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDelivery;
import org.project.ttokttok.domain.notification.mail.domain.ResultMailDeliveryStatus;
import org.project.ttokttok.domain.notification.mail.repository.ResultMailDeliveryRepository;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.exception.EmailValidationException;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "result-mail.worker.enabled=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:resultMailDelivery;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
class ResultMailDeliveryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 11, 10, 0);
    private static final Duration LEASE = Duration.ofMinutes(5);
    private static final ResultMailContent CONTENT = new ResultMailContent("합격", "축하합니다");

    @Autowired
    private ResultMailDeliveryService service;

    @Autowired
    private ResultMailDeliveryRepository repository;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    private List<String> recipients(int count) {
        return IntStream.range(0, count).mapToObj(i -> "user" + i + "@example.com").toList();
    }

    @Test
    @DisplayName("수신자 3명을 enqueue하면 PENDING 3행이 저장된다")
    void enqueue_savesOnePendingRowPerRecipient() {
        service.enqueue("req-1", "club-1", recipients(3), CONTENT, NOW);

        List<ResultMailDelivery> rows = repository.findAll();
        assertThat(rows).hasSize(3);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.PENDING);
            assertThat(row.getRequestId()).isEqualTo("req-1");
            assertThat(row.getSubject()).isEqualTo("합격");
        });
    }

    @Test
    @DisplayName("content가 null이면 아무것도 저장하지 않는다")
    void enqueue_nullContent_savesNothing() {
        service.enqueue("req-1", "club-1", recipients(3), null, NOW);

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("수신자가 비어 있으면 아무것도 저장하지 않는다")
    void enqueue_emptyRecipients_savesNothing() {
        service.enqueue("req-1", "club-1", List.of(), CONTENT, NOW);

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("claimDue는 한 번에 10건만 가져와 SENDING으로 바꾸고, 다시 부르면 남은 건만 가져온다")
    void claimDue_claimsBatchOfTenOnce() {
        service.enqueue("req-1", "club-1", recipients(12), CONTENT, NOW);

        List<ClaimedResultMail> first = service.claimDue(NOW);
        List<ClaimedResultMail> second = service.claimDue(NOW);

        assertThat(first).hasSize(10);
        assertThat(second).hasSize(2);
        assertThat(first).extracting(ClaimedResultMail::id)
                .doesNotContainAnyElementsOf(second.stream().map(ClaimedResultMail::id).toList());
        assertThat(repository.findAll()).allSatisfy(row ->
                assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.SENDING));
        assertThat(first.get(0).content()).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("아직 발송 시각이 되지 않은 작업은 가져오지 않는다")
    void claimDue_skipsNotYetDue() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);

        assertThat(service.claimDue(NOW.minusSeconds(1))).isEmpty();
    }

    @Test
    @DisplayName("recordSuccess는 SENDING 행을 SENT로 바꾼다")
    void recordSuccess_marksSent() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        String id = service.claimDue(NOW).get(0).id();

        service.recordSuccess(id, NOW.plusSeconds(1));

        ResultMailDelivery row = repository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.SENT);
        assertThat(row.getSentAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @DisplayName("recordFailure는 FAILED와 예외 클래스 이름만 last_error에 남긴다")
    void recordFailure_marksFailedWithClassNames() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        String id = service.claimDue(NOW).get(0).id();

        service.recordFailure(id,
                new RuntimeException("secret@example.com 실패", new IllegalStateException("boom")),
                NOW.plusSeconds(1));

        ResultMailDelivery row = repository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.FAILED);
        assertThat(row.getLastError()).isEqualTo("RuntimeException <- IllegalStateException");
    }

    @Test
    @DisplayName("SENDING이 아닌 행에는 recordSuccess·recordFailure를 무시한다")
    void record_ignoresRowsNotSending() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        String id = repository.findAll().get(0).getId();

        service.recordSuccess(id, NOW);
        service.recordFailure(id, new RuntimeException(), NOW);

        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo(ResultMailDeliveryStatus.PENDING);
    }

    private String saveSending(int attemptCount, LocalDateTime leaseUntil) {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        ResultMailDelivery row = repository.findAll().get(0);
        for (int i = 0; i < attemptCount; i++) {
            row.startAttempt(leaseUntil.minus(LEASE));
        }
        repository.saveAndFlush(row);
        return row.getId();
    }

    @Test
    @DisplayName("임대가 만료된 SENDING 행은 임대 끝 시각부터 다시 가져오고 시도 횟수를 늘린다")
    void claimDue_reclaimsExpiredLease() {
        LocalDateTime leaseUntil = NOW.plusMinutes(5);
        String id = saveSending(1, leaseUntil);

        assertThat(service.claimDue(leaseUntil.minusSeconds(1))).isEmpty();
        List<ClaimedResultMail> claimed = service.claimDue(leaseUntil);

        assertThat(claimed).extracting(ClaimedResultMail::id).containsExactly(id);
        ResultMailDelivery row = repository.findById(id).orElseThrow();
        assertThat(row.getAttemptCount()).isEqualTo(2);
        assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.SENDING);
        assertThat(row.getLeaseUntil()).isEqualTo(leaseUntil.plus(LEASE));
    }

    @Test
    @DisplayName("시도가 바닥난 채 임대가 만료된 행은 가져오지 않고 LEASE_EXPIRED로 FAILED 처리한다")
    void claimDue_exhaustedExpiredLease_failsWithoutReturning() {
        LocalDateTime leaseUntil = NOW.plusMinutes(5);
        String id = saveSending(5, leaseUntil);

        List<ClaimedResultMail> claimed = service.claimDue(leaseUntil);

        assertThat(claimed).isEmpty();
        ResultMailDelivery row = repository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.FAILED);
        assertThat(row.getLastError()).isEqualTo("LEASE_EXPIRED");
        assertThat(row.getAttemptCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("recordFailure: 일시 실패면 true를 반환하고 PENDING으로 되돌려 1분 뒤로 미룬다")
    void recordFailure_retryable_returnsToPending() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        String id = service.claimDue(NOW).get(0).id();
        LocalDateTime failedAt = NOW.plusSeconds(1);

        boolean willRetry = service.recordFailure(id, new MailSendException("smtp down"), failedAt);

        assertThat(willRetry).isTrue();
        ResultMailDelivery row = repository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(ResultMailDeliveryStatus.PENDING);
        assertThat(row.getNextAttemptAt()).isEqualTo(failedAt.plusMinutes(1));
        assertThat(row.getLastError()).isEqualTo("MailSendException");
    }

    @Test
    @DisplayName("recordFailure: SENDING이 아닌 행이면 false")
    void recordFailure_notSending_returnsFalse() {
        service.enqueue("req-1", "club-1", recipients(1), CONTENT, NOW);
        String id = repository.findAll().get(0).getId();

        assertThat(service.recordFailure(id, new MailSendException("x"), NOW)).isFalse();
    }

    @Test
    @DisplayName("isRetryable: 예외나 원인이 MailException이면 true, 파싱·준비 실패와 그 밖은 false")
    void isRetryable_classifiesFailures() {
        assertThat(ResultMailDeliveryService.isRetryable(new RuntimeException(new MailSendException("x")))).isTrue();
        assertThat(ResultMailDeliveryService.isRetryable(new MailSendException("x"))).isTrue();
        assertThat(ResultMailDeliveryService.isRetryable(new RuntimeException(new MailParseException("x")))).isFalse();
        assertThat(ResultMailDeliveryService.isRetryable(new RuntimeException(new MailPreparationException("x")))).isFalse();
        assertThat(ResultMailDeliveryService.isRetryable(new RuntimeException(new MessagingException("x")))).isFalse();
        assertThat(ResultMailDeliveryService.isRetryable(
                new RuntimeException(new UnsupportedEncodingException("x")))).isFalse();
        assertThat(ResultMailDeliveryService.isRetryable(
                new EmailValidationException(ErrorMessage.EMAIL_RESULT_RECIPIENT_INVALID))).isFalse();
    }
}
