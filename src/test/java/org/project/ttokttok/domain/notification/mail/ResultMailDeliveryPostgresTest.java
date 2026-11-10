package org.project.ttokttok.domain.notification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.notification.mail.scheduler.ResultMailDeliveryScheduler;
import org.project.ttokttok.domain.notification.mail.service.ResultMailDeliveryService;
import org.project.ttokttok.domain.notification.mail.service.dto.ClaimedResultMail;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.exception.EmailValidationException;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * #460 결과 메일 워커의 동시 획득·임대 만료 회수·재시도 회귀 테스트. 전용 로컬 PostgreSQL에서만 실행한다.
 * H2는 FOR UPDATE SKIP LOCKED를 흉내 낼 뿐이라 동시성은 실제 PostgreSQL에서만 확인할 수 있다.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=result_mail_delivery_regression",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO result_mail_delivery_regression",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=20",
        "result-mail.worker.enabled=false",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class ResultMailDeliveryPostgresTest {

    private static final int RECIPIENTS = 200;
    private static final int CLAIM_SIZE = 10;
    private static final ResultMailContent CONTENT = new ResultMailContent("결과 안내", "본문");

    @Autowired private ResultMailDeliveryService service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoBean private EmailService emailService;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private FCMService fcmService;

    private String clubId;

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("TTOKTTOK_CONCURRENCY_JDBC_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")) {
            throw new IllegalArgumentException("Use the dedicated local ttokttok426 test database only");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void resetTable() {
        jdbcTemplate.update("delete from result_mail_deliveries");
        clubId = createClub();
    }

    @Test
    void twoWorkersNeverClaimSameRow() throws Exception {
        LocalDateTime now = truncatedNow();
        enqueue(RECIPIENTS, now);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<List<String>> worker = () -> {
                barrier.await(30, TimeUnit.SECONDS);
                List<String> ids = new ArrayList<>();
                for (List<ClaimedResultMail> batch = service.claimDue(now); !batch.isEmpty(); batch = service.claimDue(now)) {
                    batch.forEach(mail -> ids.add(mail.id()));
                }
                return ids;
            };
            Future<List<String>> first = executor.submit(worker);
            Future<List<String>> second = executor.submit(worker);
            List<String> firstIds = first.get(60, TimeUnit.SECONDS);
            List<String> secondIds = second.get(60, TimeUnit.SECONDS);

            Set<String> union = new HashSet<>(firstIds);
            union.addAll(secondIds);
            assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            assertThat(union).hasSize(RECIPIENTS);
            assertThat(countByStatus("SENDING")).isEqualTo(RECIPIENTS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void crashedClaimIsRecoveredAfterLeaseExpiry() {
        LocalDateTime t0 = truncatedNow();
        enqueue(RECIPIENTS, t0);
        Set<String> crashed = new HashSet<>();
        service.claimDue(t0).forEach(mail -> crashed.add(mail.id())); // 기록 없이 버린다 = 처리 중 종료
        assertThat(crashed).hasSize(CLAIM_SIZE);

        LocalDateTime beforeExpiry = t0.plusMinutes(4);
        for (List<ClaimedResultMail> batch = service.claimDue(beforeExpiry); !batch.isEmpty();
             batch = service.claimDue(beforeExpiry)) {
            for (ClaimedResultMail mail : batch) {
                assertThat(crashed).as("임대가 살아 있는 행은 다시 나오지 않는다").doesNotContain(mail.id());
                service.recordSuccess(mail.id(), beforeExpiry);
            }
        }
        assertThat(countByStatus("SENT")).isEqualTo(RECIPIENTS - CLAIM_SIZE);

        LocalDateTime atExpiry = t0.plusMinutes(5);
        List<ClaimedResultMail> recovered = service.claimDue(atExpiry);
        assertThat(recovered).extracting(ClaimedResultMail::id).containsExactlyInAnyOrderElementsOf(crashed);
        assertThat(jdbcTemplate.queryForList("select attempt_count from result_mail_deliveries where status = 'SENDING'",
                Integer.class)).hasSize(CLAIM_SIZE).containsOnly(2);
        recovered.forEach(mail -> service.recordSuccess(mail.id(), atExpiry));

        assertThat(countByStatus("SENT")).isEqualTo(RECIPIENTS);
    }

    @Test
    void transientSmtpFailureIsRetriedUntilSent() {
        enqueue(CLAIM_SIZE, LocalDateTime.now());
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() <= 2 * CLAIM_SIZE) {
                throw new RuntimeException(new MailSendException("down"));
            }
            return null;
        }).when(emailService).sendResultEmail(anyString(), any());
        ResultMailDeliveryScheduler scheduler = new ResultMailDeliveryScheduler(service, emailService);

        for (int round = 1; round <= 3; round++) {
            scheduler.sendDueMails();
            if (round < 3) {
                assertThat(countByStatus("PENDING")).as("%d라운드 뒤 재시도 대기", round).isEqualTo(CLAIM_SIZE);
                jdbcTemplate.update("update result_mail_deliveries set next_attempt_at = now() - interval '1 second'");
            }
        }

        assertThat(countByStatus("SENT")).isEqualTo(CLAIM_SIZE);
        assertThat(countByStatus("FAILED")).isZero();
        assertThat(jdbcTemplate.queryForList("select attempt_count from result_mail_deliveries", Integer.class))
                .hasSize(CLAIM_SIZE).containsOnly(3);
    }

    @Test
    void permanentFailureStopsImmediately() {
        enqueue(CLAIM_SIZE, LocalDateTime.now());
        doThrow(new EmailValidationException(ErrorMessage.EMAIL_RESULT_RECIPIENT_INVALID))
                .when(emailService).sendResultEmail(anyString(), any());

        new ResultMailDeliveryScheduler(service, emailService).sendDueMails();

        assertThat(countByStatus("FAILED")).isEqualTo(CLAIM_SIZE);
        assertThat(jdbcTemplate.queryForList("select attempt_count from result_mail_deliveries", Integer.class))
                .hasSize(CLAIM_SIZE).containsOnly(1);
    }

    // PostgreSQL timestamp는 마이크로초까지라 경계값 비교가 어긋나지 않도록 초 단위로 자른다.
    private static LocalDateTime truncatedNow() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private void enqueue(int count, LocalDateTime now) {
        List<String> recipients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            recipients.add("r" + i + "@sangmyung.kr");
        }
        service.enqueue(UUID.randomUUID().toString(), clubId, recipients, CONTENT, now);
    }

    private long countByStatus(String status) {
        return jdbcTemplate.queryForObject("select count(*) from result_mail_deliveries where status = ?",
                Long.class, status);
    }

    private String createClub() {
        String[] holder = new String[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            Admin admin = Admin.adminJoin("mail" + UUID.randomUUID().toString().substring(0, 8), "test-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("mail-" + UUID.randomUUID())
                    .clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            holder[0] = club.getId();
        });
        return holder[0];
    }
}
