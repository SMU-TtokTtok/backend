package org.project.ttokttok.domain.notification.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
}
