package org.project.ttokttok.domain.applicant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.ApplicantPhase;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.exception.ResultAnnouncementException;
import org.project.ttokttok.domain.applicant.service.dto.request.ResultAnnouncementServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.request.SendResultMailServiceRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ResultAnnouncementServiceResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.notification.mail.service.ResultMailDeliveryService;
import org.project.ttokttok.global.exception.ErrorMessage;
import org.project.ttokttok.infrastructure.email.dto.ResultMailContent;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * #465 결과 발표 API의 멱등성 회귀 테스트. 전용 로컬 PostgreSQL에서만 실행한다.
 * 두 번 클릭, 응답을 못 받은 재요청, 확정·발송 도중 실패에서 중복 발송 작업과 확정·작업 불일치가 0건인지 본다.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=result_announcement_regression",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO result_announcement_regression",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=40",
        "result-mail.worker.enabled=false",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class ResultAnnouncementPostgresTest {

    private static final int PASSERS = 30;
    private static final int FAILERS = 10;
    private static final int CONCURRENT_REQUESTS = 20;
    private static final SendResultMailServiceRequest MAIL = SendResultMailServiceRequest.builder()
            .pass(new ResultMailContent("합격 안내", "축하합니다"))
            .fail(new ResultMailContent("불합격 안내", "감사합니다"))
            .build();

    private record Fixture(String clubId, String username) {}

    @Autowired private ApplicantAdminService applicantAdminService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean private ResultMailDeliveryService resultMailDeliveryService;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;

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

    @Test
    void concurrentRequestsWithTheSameKeyAnnounceOnceAndGetTheSameResult() throws Exception {
        Fixture f = createClub(-1);

        List<Object> results = runConcurrently(CONCURRENT_REQUESTS, i -> () -> announce(f, "double-click"));

        assertThat(results).allSatisfy(result -> assertThat(result).isInstanceOf(ResultAnnouncementServiceResponse.class));
        assertThat(results).hasSize(CONCURRENT_REQUESTS).containsOnly(results.get(0));
        assertThat((ResultAnnouncementServiceResponse) results.get(0))
                .extracting(ResultAnnouncementServiceResponse::passMailCount, ResultAnnouncementServiceResponse::failMailCount)
                .containsExactly(PASSERS, FAILERS);
        assertAnnouncedOnce(f, ((ResultAnnouncementServiceResponse) results.get(0)).announcementId());
    }

    @Test
    void retryAfterALostResponseReturnsTheFirstResultWithoutNewMails() {
        Fixture f = createClub(-1);

        ResultAnnouncementServiceResponse first = announce(f, "lost-response");
        ResultAnnouncementServiceResponse retried = announce(f, "lost-response");

        assertThat(retried).isEqualTo(first);
        assertAnnouncedOnce(f, first.announcementId());
    }

    @Test
    void concurrentRequestsWithDifferentKeysForTheSamePhaseAnnounceOnceAndRejectTheRest() throws Exception {
        Fixture f = createClub(-1);

        List<Object> results = runConcurrently(CONCURRENT_REQUESTS, i -> () -> announce(f, "tab-" + i));

        List<Object> announced = results.stream().filter(ResultAnnouncementServiceResponse.class::isInstance).toList();
        assertThat(announced).hasSize(1);
        assertThat(results).filteredOn(ResultAnnouncementException.class::isInstance)
                .hasSize(CONCURRENT_REQUESTS - 1)
                .allSatisfy(e -> assertThat((Throwable) e).hasMessage(ErrorMessage.RESULT_ALREADY_ANNOUNCED.getMessage()));
        assertAnnouncedOnce(f, ((ResultAnnouncementServiceResponse) announced.get(0)).announcementId());
    }

    @Test
    void failureWhileFinalizingRollsBackEverythingAndTheSameKeyCanRetry() {
        // 17번째 합격자의 연락처가 비어 있어 부원 저장(phone_number NOT NULL)이 커밋 직전에 실패한다.
        Fixture f = createClub(16);

        assertThatThrownBy(() -> announce(f, "retry-after-failure")).isInstanceOf(DataIntegrityViolationException.class);
        assertNothingAnnounced(f);

        jdbcTemplate.update("update applicants set phone = '010-1234-5678' where phone is null");

        ResultAnnouncementServiceResponse retried = announce(f, "retry-after-failure");
        assertAnnouncedOnce(f, retried.announcementId());
    }

    @Test
    void failureWhileSavingMailJobsRollsBackTheFinalization() {
        Fixture f = createClub(-1);
        doThrow(new DataAccessResourceFailureException("발송 작업 저장 실패"))
                .when(resultMailDeliveryService).enqueue(anyString(), anyString(), anyList(), any(), any());

        assertThatThrownBy(() -> announce(f, "enqueue-failure")).isInstanceOf(DataAccessResourceFailureException.class);

        assertNothingAnnounced(f);
    }

    private ResultAnnouncementServiceResponse announce(Fixture f, String key) {
        return applicantAdminService.announceResults(
                new ResultAnnouncementServiceRequest(f.username, f.clubId, ApplicantPhase.DOCUMENT, key, MAIL));
    }

    /** 발표 기록 1건, 그 발표를 가리키는 발송 작업이 수신자마다 1건, 합격자마다 부원 1명. */
    private void assertAnnouncedOnce(Fixture f, String announcementId) {
        assertThat(count("select count(*) from result_announcements where club_id = ?", f.clubId)).isEqualTo(1);
        assertThat(count("select count(*) from result_mail_deliveries where club_id = ?", f.clubId))
                .as("수신자별 발송 작업").isEqualTo(PASSERS + FAILERS);
        assertThat(count("select count(*) from result_mail_deliveries where club_id = ? and request_id <> ?",
                f.clubId, announcementId)).as("다른 발표에 묶인 발송 작업").isZero();
        assertThat(count("""
                select count(*) from (select 1 from result_mail_deliveries where club_id = ?
                group by recipient_email having count(*) > 1) d
                """, f.clubId)).as("중복 발송 작업").isZero();
        assertThat(count("select count(*) from club_members where club_id = ?", f.clubId)).isEqualTo(PASSERS);
    }

    private void assertNothingAnnounced(Fixture f) {
        assertThat(count("select count(*) from result_announcements where club_id = ?", f.clubId)).isZero();
        assertThat(count("select count(*) from result_mail_deliveries where club_id = ?", f.clubId)).isZero();
        assertThat(count("select count(*) from club_members where club_id = ?", f.clubId))
                .as("발표가 실패하면 확정도 남지 않는다").isZero();
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    /** 면접 없는 지원 폼에 서류 합격자와 불합격자가 있는 동아리. nullPhoneIndex 번째 합격자는 연락처가 없다. */
    private Fixture createClub(int nullPhoneIndex) {
        Fixture[] holder = new Fixture[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            String username = "annc" + UUID.randomUUID().toString().substring(0, 8);
            Admin admin = Admin.adminJoin(username, "test-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("annc-" + UUID.randomUUID()).clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            ApplyForm form = ApplyForm.createApplyForm(club, false,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null, null, 1000,
                    Set.of(ApplicableGrade.FIRST_GRADE), "지원폼", "설명", List.of(
                            new Question("text", "지원 동기", "설명", QuestionType.SHORT_ANSWER, true, List.of())));
            entityManager.persist(form);
            for (int i = 0; i < PASSERS + FAILERS; i++) {
                String email = "a" + i + "-" + UUID.randomUUID().toString().substring(0, 8) + "@sangmyung.kr";
                Applicant applicant = Applicant.createApplicant(email, "지원자" + i, 20, "컴퓨터공학", email,
                        i == nullPhoneIndex ? null : "010-1234-5678", StudentStatus.ENROLLED, Grade.FIRST_GRADE,
                        Gender.MALE, form);
                applicant.submitDocument(List.of());
                if (i < PASSERS) {
                    applicant.passDocumentEvaluation();
                } else {
                    applicant.failDocumentEvaluation();
                }
                entityManager.persist(applicant);
            }
            holder[0] = new Fixture(club.getId(), username);
        });
        return holder[0];
    }

    /** 요청이 던진 예외는 결과로 돌려주어 테스트가 성공/실패 분포를 직접 확인한다. */
    private List<Object> runConcurrently(int count, IntFunction<Callable<Object>> tasks) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Callable<Object> task = tasks.apply(i);
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("start timed out");
                    }
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }
}
