package org.project.ttokttok.domain.applicant.controller;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.project.ttokttok.domain.admin.domain.Admin;
import org.project.ttokttok.domain.applicant.domain.Applicant;
import org.project.ttokttok.domain.applicant.domain.enums.Gender;
import org.project.ttokttok.domain.applicant.domain.enums.Grade;
import org.project.ttokttok.domain.applicant.domain.enums.StudentStatus;
import org.project.ttokttok.domain.applicant.service.answer.AnswerAssembler;
import org.project.ttokttok.domain.applyform.domain.ApplyForm;
import org.project.ttokttok.domain.applyform.domain.enums.ApplicableGrade;
import org.project.ttokttok.domain.applyform.domain.enums.QuestionType;
import org.project.ttokttok.domain.applyform.domain.json.Question;
import org.project.ttokttok.domain.club.domain.Club;
import org.project.ttokttok.domain.club.domain.enums.ClubUniv;
import org.project.ttokttok.domain.temp.applicant.domain.TempApplicant;
import org.project.ttokttok.domain.user.domain.User;
import org.project.ttokttok.global.entity.Role;
import org.project.ttokttok.infrastructure.email.service.EmailService;
import org.project.ttokttok.infrastructure.firebase.service.FCMService;
import org.project.ttokttok.infrastructure.jwt.JwtFactory;
import org.project.ttokttok.infrastructure.s3.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Dedicated disposable PostgreSQL schema; models the release after the UNIQUE constraint is added. */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=duplicate_apply_regression",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "spring.datasource.hikari.connection-init-sql=SET search_path TO duplicate_apply_regression",
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=30"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TTOKTTOK_CONCURRENCY_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:\\d+/ttokttok426")
class ApplicantDuplicateApplyPostgresTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtFactory jwtFactory;
    @MockitoBean private S3Service s3Service;
    @MockitoBean private EmailService emailService;
    @MockitoBean private FCMService fcmService;
    @MockitoSpyBean private AnswerAssembler answerAssembler;

    private String email;
    private String clubId;
    private String formId;

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
    void setUp() {
        if (!Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists(select 1 from pg_constraint
                where conrelid = 'applicants'::regclass and conname = 'uk_applicants_user_email_applyform')
                """, Boolean.class))) {
            jdbcTemplate.execute("alter table applicants add constraint uk_applicants_user_email_applyform "
                    + "unique (user_email, applyform_id)");
        }
        email = UUID.randomUUID() + "@sangmyung.kr";
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            entityManager.persist(User.signUp(email, "test-password", "지원자", true));
            Admin admin = Admin.adminJoin("race" + UUID.randomUUID().toString().substring(0, 8), "test-password");
            entityManager.persist(admin);
            Club club = Club.builder().admin(admin).clubName("race-" + UUID.randomUUID()).clubUniv(ClubUniv.ENGINEERING).build();
            entityManager.persist(club);
            ApplyForm form = ApplyForm.createApplyForm(club, false,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), null, null, 100,
                    Set.of(ApplicableGrade.FIRST_GRADE), "지원폼", "설명", List.of(
                            new Question("text", "지원 동기", "설명", QuestionType.SHORT_ANSWER, true, List.of())));
            entityManager.persist(form);
            clubId = club.getId();
            formId = form.getId();
        });
    }

    @Test
    void sequentialDuplicateIsRejected() throws Exception {
        List<Integer> statuses = List.of(submit(), submit());

        assertThat(statuses).containsExactly(200, 409);
        assertSingleApplication("sequential-2", statuses);
    }

    @Test
    void overlappingRequestsMustNotBothPersist() throws Exception {
        CyclicBarrier afterDuplicateCheck = new CyclicBarrier(2);
        // Pause after the real duplicate query, then run the real assembly; no repository result is faked.
        doAnswer(invocation -> {
            afterDuplicateCheck.await(15, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(answerAssembler).assemble(any(), any(), any());

        List<Integer> statuses = submitConcurrently(2);

        assertThat(statuses).allMatch(status -> status == 200 || status == 409);
        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertSingleApplication("overlap-2", statuses);
    }

    @Test
    void twentyRequestsMustPersistOnlyOneApplication() throws Exception {
        List<Integer> statuses = submitConcurrently(20);

        assertThat(statuses).allMatch(status -> status == 200 || status == 409);
        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
        assertSingleApplication("start-together-20", statuses);
    }

    @Test
    void differentUsersCanApplyToTheSameFormConcurrently() throws Exception {
        String otherEmail = UUID.randomUUID() + "@sangmyung.kr";
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                entityManager.persist(User.signUp(otherEmail, "test-password", "다른 지원자", true)));
        CyclicBarrier afterDuplicateCheck = new CyclicBarrier(2);
        doAnswer(invocation -> {
            afterDuplicateCheck.await(15, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(answerAssembler).assemble(any(), any(), any());
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> submit());
            Future<Integer> second = executor.submit(() -> submit(otherEmail));
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactly(200, 200);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from applicants where applyform_id = ?",
                Long.class, formId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from document_phases d join applicants a on a.id = d.applicant_id
                where a.applyform_id = ?
                """, Long.class, formId)).isEqualTo(2);
        verifyNoInteractions(s3Service, emailService, fcmService);
    }

    @Test
    void uniqueConflictRollsBackTemporaryApplicationDeletion() throws Exception {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored ->
                entityManager.persist(TempApplicant.create(formId, email, Map.of("draft", "keep"))));
        doAnswer(invocation -> {
            // A competing transaction commits after this request's real duplicate query.
            TransactionTemplate competitor = new TransactionTemplate(transactionManager);
            competitor.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            competitor.executeWithoutResult(ignored -> {
                Applicant winner = Applicant.createApplicant(email, "지원자", 20, "컴퓨터공학", email,
                        "010-1234-5678", StudentStatus.ENROLLED, Grade.FIRST_GRADE, Gender.MALE,
                        entityManager.getReference(ApplyForm.class, formId));
                winner.submitDocument(List.of());
                entityManager.persist(winner);
            });
            return invocation.callRealMethod();
        }).when(answerAssembler).assemble(any(), any(), any());

        int status = submit();

        assertThat(status).isEqualTo(409);
        assertSingleApplication("unique-conflict-draft-rollback", List.of(status));
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from temp_applicants where user_email = ? and form_id = ?
                """, Long.class, email, formId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select temp_data ->> 'draft' from temp_applicants where user_email = ? and form_id = ?
                """, String.class, email, formId)).isEqualTo("keep");
    }

    private List<Integer> submitConcurrently(int count) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent request start timed out");
                    }
                    return submit();
                }));
            }
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private int submit() throws Exception {
        return submit(email);
    }

    private int submit(String userEmail) throws Exception {
        String body = """
                {"name":"지원자","age":20,"major":"컴퓨터공학","email":"%s",
                 "phone":"010-1234-5678","studentStatus":"ENROLLED","grade":"FIRST_GRADE",
                 "gender":"MALE","applyFormId":"%s","answers":[{"questionId":"text","value":"지원합니다"}]}
                """.formatted(userEmail, formId);
        return mockMvc.perform(multipart("/api/user/applies/{clubId}", clubId)
                .file(new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE,
                        body.getBytes(StandardCharsets.UTF_8)))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtFactory.generateValidToken(userEmail, Role.ROLE_USER)))
                .andReturn().getResponse().getStatus();
    }

    private void assertSingleApplication(String scenario, List<Integer> statuses) {
        long applicants = jdbcTemplate.queryForObject(
                "select count(*) from applicants where user_email = ? and applyform_id = ?", Long.class, email, formId);
        long documents = jdbcTemplate.queryForObject("""
                select count(*) from document_phases d join applicants a on a.id = d.applicant_id
                where a.user_email = ? and a.applyform_id = ?
                """, Long.class, email, formId);
        System.out.printf("DUPLICATE_APPLY_REGRESSION scenario=%s statuses=%s applicants=%d documents=%d%n",
                scenario, statuses, applicants, documents);
        System.out.println("APPLICANTS_CONSTRAINTS " + jdbcTemplate.queryForList("""
                select contype, pg_get_constraintdef(oid) as definition from pg_constraint
                where conrelid = 'applicants'::regclass order by contype, conname
                """));
        verifyNoInteractions(s3Service, emailService, fcmService);
        assertThat(applicants).as("same user/form must have one application").isEqualTo(1);
        assertThat(documents).as("same user/form must have one document phase").isEqualTo(1);
    }
}
