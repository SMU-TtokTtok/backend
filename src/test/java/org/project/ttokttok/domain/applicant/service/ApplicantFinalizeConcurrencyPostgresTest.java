package org.project.ttokttok.domain.applicant.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.service.dto.request.ApplicantFinalizationRequest;
import org.project.ttokttok.domain.applicant.service.dto.response.ApplicantFinalizeServiceResponse;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.clubMember.exception.AlreadyClubMemberException;
import org.project.ttokttok.domain.clubMember.service.ClubMemberService;
import org.project.ttokttok.domain.clubMember.service.dto.request.ClubMemberServiceRequest;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #432 합격 확정의 반복·동시 요청 멱등성 회귀 테스트. 전용 로컬 PostgreSQL에서만 실행한다.
 * club_members에 UNIQUE 제약이 없는 상태(코드 선배포 릴리스)에서 동아리 행 잠금만으로 통과해야 한다.
 */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=finalize_idempotency_regression",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO finalize_idempotency_regression",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=40",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class ApplicantFinalizeConcurrencyPostgresTest {

    private static final int PASSERS = 30;
    private static final int CONCURRENT_REQUESTS = 20;
    private static final long MANUAL_STUDENT_NUM = 60201234L;

    private record Fixture(String clubId, String username) {}

    @Autowired private ApplicantAdminService applicantAdminService;
    @Autowired private ClubMemberService clubMemberService;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
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
    void concurrentFinalizeRegistersEachPasserOnceWithTheSameResult() throws Exception {
        Fixture f = createClub(PASSERS, -1, null);

        List<Object> results = runConcurrently(CONCURRENT_REQUESTS, i -> () -> finalizeOnce(f));

        assertThat(results).allSatisfy(result -> assertThat(result).isEqualTo(new ApplicantFinalizeServiceResponse(PASSERS, PASSERS)));
        assertOneMemberPerPasser(f, PASSERS);
    }

    @Test
    void repeatedFinalizeReturnsTheSameResultWithoutDuplicates() {
        Fixture f = createClub(PASSERS, -1, null);

        ApplicantFinalizeServiceResponse first = finalizeOnce(f);
        ApplicantFinalizeServiceResponse second = finalizeOnce(f);
        ApplicantFinalizeServiceResponse third = finalizeOnce(f);

        assertThat(List.of(second, third)).containsOnly(first);
        assertThat(first).isEqualTo(new ApplicantFinalizeServiceResponse(PASSERS, PASSERS));
        assertOneMemberPerPasser(f, PASSERS);
    }

    @Test
    void failedFinalizeRollsBackEverythingAndRetryRecovers() {
        // 17번째 합격자의 연락처가 비어 있어 부원 저장(phone_number NOT NULL)이 중간에 실패한다.
        Fixture f = createClub(PASSERS, 16, null);

        assertThatThrownBy(() -> finalizeOnce(f)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(memberCount(f)).as("실패한 확정은 일부 부원만 남기지 않는다").isZero();

        jdbcTemplate.update("update applicants set phone = '010-1234-5678' where phone is null");

        assertThat(finalizeOnce(f)).isEqualTo(new ApplicantFinalizeServiceResponse(PASSERS, PASSERS));
        assertOneMemberPerPasser(f, PASSERS);
    }

    @Test
    void finalizeAndManualAddForTheSameEmailRegisterOnlyOneMember() throws Exception {
        String sharedEmail = MANUAL_STUDENT_NUM + "@sangmyung.kr";
        Fixture f = createClub(1, -1, sharedEmail);

        List<Object> results = runConcurrently(CONCURRENT_REQUESTS, i -> i % 2 == 0
                ? () -> finalizeOnce(f)
                : () -> manualAdd(f));

        assertThat(results).allSatisfy(result -> assertThat(result)
                .isInstanceOfAny(ApplicantFinalizeServiceResponse.class, String.class, AlreadyClubMemberException.class));
        assertThat(memberCount(f)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(distinct email) from club_members where club_id = ?", Long.class, f.clubId)).isEqualTo(1);
    }

    private ApplicantFinalizeServiceResponse finalizeOnce(Fixture f) {
        return applicantAdminService.finalizeApplicantsStatus(
                ApplicantFinalizationRequest.of(f.username, f.clubId, "DOCUMENT"));
    }

    /** 성공하면 부원 ID, 이미 등록돼 있으면 기존 규약의 AlreadyClubMemberException을 반환값으로 돌려준다. */
    private Object manualAdd(Fixture f) {
        try {
            return clubMemberService.addMember(f.username, f.clubId, ClubMemberServiceRequest.builder()
                    .studentNum(MANUAL_STUDENT_NUM)
                    .name("수동 등록")
                    .major("컴퓨터공학과")
                    .grade(Grade.FIRST_GRADE)
                    .phoneNumber("010-1111-1111")
                    .gender(Gender.MALE)
                    .build(), "MEMBER");
        } catch (AlreadyClubMemberException e) {
            return e;
        }
    }

    private void assertOneMemberPerPasser(Fixture f, int expectedMembers) {
        assertThat(memberCount(f)).isEqualTo(expectedMembers);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from (select 1 from club_members where club_id = ?
                group by email having count(*) > 1) d
                """, Long.class, f.clubId)).as("중복 부원 그룹").isZero();
    }

    private long memberCount(Fixture f) {
        return jdbcTemplate.queryForObject("select count(*) from club_members where club_id = ?", Long.class, f.clubId);
    }

    /** 합격자(서류 전형 PASS)가 있는 동아리. nullPhoneIndex 번째 합격자는 연락처가 없고, firstEmail이 있으면 첫 합격자 이메일로 쓴다. */
    private Fixture createClub(int passers, int nullPhoneIndex, String firstEmail) {
        Fixture[] holder = new Fixture[1];
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            String username = "idem" + UUID.randomUUID().toString().substring(0, 8);
            Admin admin = Admin.adminJoin(username, "test-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("idem-" + UUID.randomUUID()).clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            ApplyForm form = ApplyForm.createApplyForm(club, false,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null, null, 1000,
                    Set.of(ApplicableGrade.FIRST_GRADE), "지원폼", "설명", List.of(
                            new Question("text", "지원 동기", "설명", QuestionType.SHORT_ANSWER, true, List.of())));
            entityManager.persist(form);
            for (int i = 0; i < passers; i++) {
                String email = i == 0 && firstEmail != null
                        ? firstEmail
                        : "p" + i + "-" + UUID.randomUUID().toString().substring(0, 8) + "@sangmyung.kr";
                Applicant applicant = Applicant.createApplicant(email, "합격자" + i, 20, "컴퓨터공학", email,
                        i == nullPhoneIndex ? null : "010-1234-5678", StudentStatus.ENROLLED, Grade.FIRST_GRADE,
                        Gender.MALE, form);
                applicant.submitDocument(List.of());
                applicant.passDocumentEvaluation();
                entityManager.persist(applicant);
            }
            holder[0] = new Fixture(club.getId(), username);
        });
        return holder[0];
    }

    /** 요청이 던진 예외는 결과로 돌려주어 테스트가 성공/실패 분포를 직접 확인한다. */
    private List<Object> runConcurrently(int count, java.util.function.IntFunction<Callable<Object>> tasks) throws Exception {
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
